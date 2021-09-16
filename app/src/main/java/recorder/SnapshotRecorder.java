package recorder;

import android.media.Image;

import com.example.CS_IU_proto_1.Plane;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class SnapshotRecorder {

    private enum State {
        Idle, Start, Recording, Stop
    };
    private State state = State.Idle;

    private File currentSnapshotFile = null;
    private BufferedOutputStream os = null;
    String snapshotName = null;

    private static final String SnapshotVersion = "SNAP 1.0";
    private static final String SnapshotType = "YUV420_N12";

    private static final int StringLen = 16;

    private static final int offset_META_1 = 0;
    private static final int offset_META_2 = StringLen;
    private static final int offset_RES_W  = StringLen * 2;
    private static final int offset_RES_H  = offset_RES_W + 4;
    private static final int offset_FSIZE  = offset_RES_W + 8;

    private static final int PlaneBytes = 4 * 3 * 6;
    private static final int offset_PLANE  = offset_FSIZE + 4;
    private static final int offset_PROJMX = offset_PLANE + PlaneBytes;

    private static final int HeaderBytes = offset_PROJMX + 4 * 16;

    public boolean startRecording(File path) {
        if (state != State.Idle) return false;

        state = State.Start;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        snapshotName = "snapshot_" + sdf.format(new Date()) + ".snap";
        currentSnapshotFile = new File(path, snapshotName);

        try {
            FileOutputStream fos = new FileOutputStream(currentSnapshotFile);
            os = new BufferedOutputStream(fos);

        } catch (FileNotFoundException e) {
            state = State.Idle;
            currentSnapshotFile = null;
            e.printStackTrace();
        }

        return state == State.Start;
    }

    public boolean stopRecording() {
        if (state != State.Recording) return false;

        state = State.Stop;

        try {
            os.close();

            os = null;
            currentSnapshotFile = null;
            state = State.Idle;
        } catch (IOException e) {
            e.printStackTrace();

            state = State.Recording;
        }

        return state == State.Idle;
    }

    public boolean isRecording() {
        return state == State.Recording;
    }

    public boolean isSafeToInterrupt() {
        return state == State.Idle || state == State.Recording;
    }

    public void write(Plane plane, float[] projMX, float[] viewMX, float[] camTrans, Image image) {

        try {
            switch (state) {
                case Start:
                    writeHeader(image.getWidth(), image.getHeight(), plane, projMX);
                case Recording:
                    writeVariables(viewMX, camTrans, image);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    private final byte[] headerBuffer = new byte[HeaderBytes];
    private byte[] frameBuffer = null;

    private void writeHeader(int w, int h, Plane plane, float[] proj) throws IOException {

        int frameSize = (w * h * 3 / 2) + (4 * 16) + (4 * 3); // img + viewMX + camTrans
        frameBuffer = new byte[frameSize];

        float[] ll, lr, ul, ur;
        float[] normal;
        float[] center;

        ul = plane.ul; ur = plane.ur;
        ll = plane.ll; lr = plane.lr;

        normal = plane.normal;
        center = plane.center;

        ByteBuffer wrapper = ByteBuffer.wrap(headerBuffer);
        wrapper.order(ByteOrder.LITTLE_ENDIAN);

        loadToBuffer(wrapper, SnapshotVersion, offset_META_1);
        loadToBuffer(wrapper,    SnapshotType, offset_META_2);
        wrapper.putInt(offset_RES_W, w);
        wrapper.putInt(offset_RES_H, h);
        wrapper.putInt(offset_FSIZE, frameSize);

        loadToBuffer(wrapper,     ll, offset_PLANE);
        loadToBuffer(wrapper,     lr, -1);
        loadToBuffer(wrapper,     ul, -1);
        loadToBuffer(wrapper,     ur, -1);
        loadToBuffer(wrapper, normal, -1);
        loadToBuffer(wrapper, center, -1);

        loadToBuffer(wrapper,   proj, offset_PROJMX);
        os.write(headerBuffer);

        state = State.Recording;
    }

    private void writeVariables(float[] viewMX, float[] camTrans, Image image) throws IOException {
        Arrays.fill(frameBuffer, (byte) 0);
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer bufferY   = planes[0].getBuffer();
        ByteBuffer bufferUV  = planes[1].getBuffer();

        ByteBuffer wrapper = ByteBuffer.wrap(frameBuffer);
        wrapper.order(ByteOrder.LITTLE_ENDIAN);

        loadToBuffer(wrapper, viewMX, -1);
        loadToBuffer(wrapper, camTrans, -1);
        wrapper.put(bufferY);
        wrapper.put(bufferUV);

        os.write(frameBuffer);
    }

    private static void loadToBuffer(ByteBuffer buffer, String value, int offset) {
        byte[] bin = value.getBytes(StandardCharsets.UTF_8);

        if (offset != -1) buffer.position(offset);

        int i = 0;
        for (; i < StringLen && i < bin.length; i++) {
            buffer.put(bin[i]);
        }
        for (; i < StringLen; i++) {
            buffer.put((byte) 0 );
        }
    }

    private static void loadToBuffer(ByteBuffer buffer, float[] values, int offset) {
        if (offset != -1) buffer.position(offset);

        for (float f : values) {
            buffer.putFloat(f);
        }
    }

}
