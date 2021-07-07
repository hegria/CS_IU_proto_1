package com.example.CS_IU_proto_1;

import com.google.ar.core.PointCloud;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class PointCollector {
  final float FINE_CONFIDENCE = 0.3f;
  Map<Integer, ArrayList<float[]>> allPoints = new HashMap<>();
  Map<Integer, float[]> filteredPoints = new HashMap<>();
  boolean isFiltered = false;

  FloatBuffer pointsBuffer;

  public void push(PointCloud pointCloud) {
    isFiltered = false;
    pointsBuffer = null;

    IntBuffer ids = pointCloud.getIds();
    FloatBuffer fb = pointCloud.getPoints();
    for (int i = 0; i < fb.capacity() / 4; i++) {
      float[] points = {fb.get(i * 4), fb.get(i * 4 + 1), fb.get(i * 4 + 2), fb.get(i * 4 + 3)};

      if (points[3] < FINE_CONFIDENCE) continue;

      int id = ids.get(i);
      if (!allPoints.containsKey(id)) {
        ArrayList<float[]> list = new ArrayList<>();
        list.add(points);
        allPoints.put(id, list);
      } else {
        Objects.requireNonNull(allPoints.get(id)).add(points);
      }
    }
  }

  public void filter() {
    for (int id : allPoints.keySet()) {
      ArrayList<float[]> list = allPoints.get(id);
      float meanX = 0f, meanY = 0f, meanZ = 0f;
      if (list == null) continue;
      for (float[] p : list) {
        meanX += p[0];
        meanY += p[1];
        meanZ += p[2];
      }
      meanX /= list.size();
      meanY /= list.size();
      meanZ /= list.size();

      if (list.size() < 5) {
        float[] finalPoint = new float[]{meanX, meanY, meanZ};
        filteredPoints.put(id, finalPoint);
        continue;
      }

      float distanceMean = 0f;
      float variance = 0f;
      for (float[] point : list) {
        float sqDist = (float) (Math.pow((point[0] - meanX), 2.0) + Math.pow((point[1] - meanY), 2.0) + Math.pow((point[2] - meanZ), 2.0));
        variance += sqDist;
        distanceMean += Math.sqrt(sqDist);
      }
      distanceMean /= list.size();
      variance = (variance / list.size()) - distanceMean * distanceMean;

      if (variance == 0) {
        float[] finalPoint = new float[]{meanX, meanY, meanZ};
        filteredPoints.put(id, finalPoint);
        continue;
      }

      Iterator<float[]> iter = list.iterator();
      while (iter.hasNext()) {
        float[] tmp = iter.next();
        float sqDistance = (float) (Math.pow((tmp[0] - meanX), 2) + Math.pow((tmp[1] - meanY), 2) + Math.pow((tmp[2] - meanZ), 2));
        float z_score = (float) (Math.abs(Math.sqrt(sqDistance) - distanceMean) / Math.sqrt(variance));
        if (z_score >= 1.2f) {
          iter.remove();
        }
      }

      if (list.size() == 0) continue;

      meanX = 0.f;
      meanY = 0.f;
      meanZ = 0.f;
      for (float[] tmp : list) {
        meanX += tmp[0];
        meanY += tmp[1];
        meanZ += tmp[2];
      }
      meanX /= list.size();
      meanY /= list.size();
      meanZ /= list.size();

      filteredPoints.put(id, new float[]{meanX, meanY, meanZ});
    }
    isFiltered = true;
  }

  public FloatBuffer getPointBuffer() {
    if (!isFiltered) filter();
    if (pointsBuffer != null) return pointsBuffer;

    pointsBuffer = ByteBuffer.allocateDirect(filteredPoints.size() * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    for (int id : filteredPoints.keySet()) {
      float[] tmp = {
              Objects.requireNonNull(filteredPoints.get(id))[0],
              Objects.requireNonNull(filteredPoints.get(id))[1],
              Objects.requireNonNull(filteredPoints.get(id))[2],
              1.0f,
      };
      pointsBuffer.put(tmp);
    }
    pointsBuffer.position(0);
    return pointsBuffer;
  }
}
