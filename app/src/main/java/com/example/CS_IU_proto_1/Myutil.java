package com.example.CS_IU_proto_1;


import android.media.Image;
import android.opengl.Matrix;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Myutil {

    //생성된 Ray와 평면의 교차점 Return
    public static float[] pickSurfacePoints(Plane plane, Ray ray){
        // 예외 처리 하기
        float[] output = new float[3];

        float parameter = (plane.dval - plane.normal[0]*ray.origin[0] - plane.normal[1]*ray.origin[1] -plane.normal[2]*ray.origin[2])
                / (plane.normal[0]*ray.dir[0]+plane.normal[1]*ray.dir[1]+plane.normal[2]*ray.dir[2]);

        output[0] = ray.dir[0]*parameter + ray.origin[0];
        output[1] = ray.dir[1]*parameter + ray.origin[1];
        output[2] = ray.dir[2]*parameter + ray.origin[2];

        return output;

    }

    // 선택한 점을 향하는 Ray 생성
    // TODO 최적화 더 할 수 있을듯???????
    public static Ray GenerateRay(float xPx, float yPx, int screenWidth, int screenHeight,float[] projMX,float[] viewMX, float[] camera_trans) {
        // https://antongerdelan.net/opengl/raycasting.html 참고

        // screen space 에서 clip space 로
        float x = 2.0f * xPx / screenWidth - 1.0f;
        float y = 1.0f - 2.0f * yPx / screenHeight;

        float[] inverseProjMX = new float[16];
        Matrix.invertM(inverseProjMX, 0, projMX, 0);

        float[] inverseViewMX = new float[16];
        Matrix.invertM(inverseViewMX, 0, viewMX, 0);

        float[] ray_clip = new float[]{x, y, -1f, 1f};

        // clip space 에서 view space 로
        float[] ray_eye = new float[4];
        Matrix.multiplyMV(ray_eye, 0, inverseProjMX, 0, ray_clip, 0);
        ray_eye = new float[]{ray_eye[0], ray_eye[1], -1.0f, 0.0f};

        // view space 에서 world space 로
        float[] ray_wor = new float[4];
        Matrix.multiplyMV(ray_wor, 0, inverseViewMX, 0, ray_eye, 0);

        // normalize 시켜주기 위해 벡터의 크기 계산
        float ray_wor_length = (float) Math.sqrt(ray_wor[0] * ray_wor[0] + ray_wor[1] * ray_wor[1] + ray_wor[2] * ray_wor[2]);

        float[] out = new float[6];

        // 카메라의 world space 좌표
        out[0] = camera_trans[0];
        out[1] = camera_trans[1];
        out[2] = camera_trans[2];

        // ray의 normalized 된 방향벡터
        out[3] = ray_wor[0] / ray_wor_length;
        out[4] = ray_wor[1] / ray_wor_length;
        out[5] = ray_wor[2] / ray_wor_length;
        Ray ray = new Ray(out);
        return ray;
    }

    public static Mat ArImg2CVImg(Image image){

        Image.Plane[] planes = image.getPlanes();

        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferUV = planes[1].getBuffer();

        ByteBuffer bufferYUV = ByteBuffer.allocateDirect( (bufferY.remaining() +  bufferUV.remaining()) );

        bufferYUV.put( bufferY ).put( bufferUV );

        // bufferYUV = > Input FIX!!!!!!!!!!!!!!!!!
        // List<Contour>  -> output FIX!!!!!!

        // Contour

        Mat inImg = new Mat( image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        inImg.put(0,0, bufferYUV.array());


        return inImg;
    }

    public static ArrayList<Contour> findCircle(Image image){
        Image.Plane[] planes = image.getPlanes();

        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferUV = planes[1].getBuffer();

        ByteBuffer bufferYUV = ByteBuffer.allocateDirect( (bufferY.remaining() +  bufferUV.remaining()) );

        bufferYUV.put( bufferY ).put( bufferUV );

        return openCVProcessing(bufferYUV, image.getHeight(), image.getWidth());
    }

    private static ArrayList<Contour> openCVProcessing (ByteBuffer bb, float height, float width){

        ArrayList<Contour> contours = new ArrayList<>();
        Contour tmpContour1 = new Contour(new float[]{0.4f, 0.4f, 0.4f, -0.4f, -0.4f, -0.4f, -0.4f, 0.4f});
        Contour tmpContour2 = new Contour(new float[]{0.6f, 0.6f, 0.6f, -0.6f, -0.6f, -0.6f, -0.6f, 0.6f});
        contours.add(tmpContour1);
        contours.add(tmpContour2);

        return contours;
    }

    public static RectEllipseSize elliToRect (Ellipse elli){

        float[] temp = new float[2];
        float[] rect_ll = new float[2];
        float[] rect_lr = new float[2];
        float[] rect_ul = new float[2];
        float[] rect_ur = new float[2];

//        temp[0] = elli.longvertex[0] - elli.cp[0];
//        temp[1] = elli.longvertex[1] - elli.cp[1];
//
//        rect_ur[0] = elli.shortvertex[0] + temp[0];
//        rect_ur[1] = elli.shortvertex[1] + temp[1];
//
//        rect_ul[0] = elli.shortvertex[0] - temp[0];
//        rect_ul[1] = elli.shortvertex[1] - temp[1];
//
//        temp[0] = elli.shortvertex[0] - elli.cp[0];
//        temp[1] = elli.shortvertex[1] - elli.cp[1];

        rect_lr[0] = rect_ur[0] - temp[0]*2;
        rect_lr[1] = rect_ur[1] - temp[1]*2;

        rect_ll[0] = rect_ul[0] - temp[0]*2;
        rect_ll[1] = rect_ul[1] - temp[1]*2;

        return new RectEllipseSize(rect_ll, rect_lr, rect_ul, rect_ur);
    }

    public static Ellipse rectToElli (RectEllipseSize rect) {
        float elli_r1, elli_r2;
        float[] elli_cp = new float[2];
        float[] elli_p1 = new float[2];
        float[] elli_p2 = new float[2];


        elli_p1[0] = (rect.ul[0] + rect.ur[0]) / 2f;
        elli_p1[1] = (rect.ul[1] + rect.ur[1]) / 2f;

        elli_p2[0] = (rect.ur[0] + rect.lr[0]) / 2f;
        elli_p2[1] = (rect.ur[1] + rect.lr[1]) / 2f;

        elli_cp[0] = (rect.ll[0] + rect.lr[0] + rect.ul[0] + rect.ur[0]) / 4f;
        elli_cp[1] = (rect.ll[1] + rect.lr[1] + rect.ul[1] + rect.ur[1]) / 4f;

        elli_r1 = (float) Math.sqrt(Math.pow(elli_cp[0] - elli_p1[0], 2) + Math.pow(elli_cp[1] - elli_p1[1], 2));
        elli_r2 = (float) Math.sqrt(Math.pow(elli_cp[0] - elli_p2[0], 2) + Math.pow(elli_cp[1] - elli_p2[1], 2));

        if (elli_r1 > elli_r2)
            return new Ellipse(elli_r1, elli_r2, elli_cp, elli_p1, elli_p2);
        else
            return new Ellipse(elli_r2, elli_r1, elli_cp, elli_p2, elli_p1);
    }

    //TODO Contour를 RectEllipseSize로 변환할 필요는 있어보임?
    public static Ellipse findBoundingBox(Contour contour){

        // FIND CENTER OF MASS

        float[] COM = new float[2];
        for(int i =0; i< contour.points.length/2; i++){
            COM[0] += contour.points[2*i];
            COM[1] += contour.points[2*i+1];
        }
        COM[0] /= contour.points.length/2f;
        COM[1] /= contour.points.length/2f;

        float xx = 0;
        float xy = 0;
        float yy = 0;
        float[] temp = new float[2];
        for(int i = 0; i<contour.points.length/2;i++){
            temp[0] = contour.points[2*i] - COM[0];
            temp[1] = contour.points[2*i+1] - COM[1];

            xx+= temp[0] * temp[0];
            xy+= temp[0] * temp[1];
            yy+= temp[1] * temp[1];
        }

//         2D MAT
//        float[][] M = new float[][]{
//                {xx, xy},
//                {xy, yy},
//        };

        //
        // -> Start SVD
        //

        // TM -> M*M^T를 곱한 Matrix > SVD 에서 쓰임

        float[][] TM = new float[][]{
                {xx*xx+xy*xy, xx*xy+xy*yy},
                {xx*xy+xy*yy, xy*xy+yy*yy}
        };

        // Lamda에 대한 2차 방정식의 해.
        // B랑 C

        float b = TM[0][0]+TM[1][1];
        float c = TM[0][0]*TM[1][1] - TM[0][1]*TM[0][1];
        float det = (float)Math.sqrt(b*b-4*c);

        float lamda1 = (b + det)/2;
        float lamda2 = (b - det)/2;

        // lamda1 > lamda2 -> UM x1, x2의 비율에 대한
        // a-lamda x1 = -b x2
        // (lamda 1) x2 , (lamda 2) x2
        // (lamda 1) x1 , (lamda 2) x1

        // TODO 내가 봤을댄 U1만 구하면 될듯??

        // UM => 내림차순 vector 두개.
        float[][] UM = new float[][]{
                {1,1},
                {-(TM[0][0]-lamda1)/TM[0][1],-(TM[0][0]-lamda2)/TM[0][1]}
        };

        //Gram-Schmidt의 orthonormalization 과정

        float dis = (float) Math.sqrt(1+UM[1][0]*UM[1][0]);
        float[] u1 = new float[]{1/dis, UM[1][0]/dis};
        float cross = u1[0]*UM[0][1]+u1[1]*UM[1][1];
        float[] u2 = new float[]{
                UM[0][1]- cross*u1[0], UM[1][1] - cross*u1[1]
        };
        dis = (float) Math.sqrt(u2[0]*u2[0]+u2[1]*u2[1]);

        // 최종 realU Matrix

        float[][] realUM = new float[][]{
                {u1[0],u2[0]/dis},
                {u1[1],u2[1]/dis}
        };

        // x축을 lamda가 큰 것으로 지정.

        float[] M_axisx = new float[]{realUM[0][0], realUM[1][0]};
        float[] M_axisy = new float[]{realUM[0][1], realUM[1][1]};

        //nomalize


        // 축 방향조정 -> Right-Handed

        float crossRst = M_axisx[0]*M_axisy[1] - M_axisx[1]*M_axisy[0];

        if(crossRst < 0){
            M_axisx[0] = -M_axisx[0];
            M_axisx[1] = -M_axisx[1];
        }

        //Bounding Box 계산.

        float xmax = 0;
        float xmin = 0;
        float ymax = 0;
        float ymin = 0;

        for(int i = 0; i<contour.points.length/2;i++){

            float[] tempp = new float[]{(contour.points[2*i]-COM[0])*M_axisx[0]+(contour.points[2*i+1]-COM[1])*M_axisx[1],
                    (contour.points[2*i]-COM[0])*M_axisy[0]+(contour.points[2*i+1]-COM[1])*M_axisy[1]};
            if(i== 0){
                xmax = tempp[0];
                xmin = tempp[0];
                ymax = tempp[1];
                ymin = tempp[1];
            }
            if(tempp[0]>xmax)
                xmax = tempp[0];
            else if(tempp[0]<xmin)
                xmin = tempp[0];
            if(tempp[1]>ymax)
                ymax = tempp[1];
            else if(tempp[1]<ymin)
                ymin = tempp[1];
        }
        float dx = (xmax-xmin)/2;
        float dy = (ymax-ymin)/2;
        float px = (xmax+xmin)/2;
        float py = (ymax+ymin)/2;

        float[] pivot = new float[]{COM[0]+px*M_axisx[0]+py*M_axisy[0],COM[1]+px*M_axisx[1]+py*M_axisy[1],0};




        // LOCAL Bounding BOX

//        float[] LL = new float[]{COM[0]+xmin*M_axisx[0]+ymin*M_axisy[0],COM[1]+xmin*M_axisx[1]+ymin*M_axisy[1]};
//        float[] LR = new float[]{COM[0]+xmax*M_axisx[0]+ymin*M_axisy[0],COM[1]+xmax*M_axisx[1]+ymin*M_axisy[1]};
//        float[] UL = new float[]{COM[0]+xmin*M_axisx[0]+ymax*M_axisy[0],COM[1]+xmin*M_axisx[1]+ymax*M_axisy[1]};
//        float[] UR = new float[]{COM[0]+xmax*M_axisx[0]+ymax*M_axisy[0],COM[1]+xmax*M_axisx[1]+ymax*M_axisy[1]};
//
//        return new Contour(new float[]{LL[0],LL[1],LR[0],LR[1],UR[0],UR[1],UL[0],UL[1]});
        return new Ellipse(dx,dy,pivot,M_axisx,M_axisy);
    }
    
//    public static EllipseSize findElipses ( Contour localContour){
//
//    }
//    public static RectEllipseSize findElipses ( Contour localContour){
//
//    }
}
