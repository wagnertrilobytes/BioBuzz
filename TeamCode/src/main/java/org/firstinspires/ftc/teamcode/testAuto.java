package org.firstinspires.ftc.teamcode;

import android.graphics.Canvas;
import android.util.Size;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


@Autonomous(name="Auto detection / DONT USE", group="Linear OpMode")
public class testAuto extends LinearOpMode {

    private DcMotor frontLeftDrive = null;
    private DcMotor backLeftDrive = null;
    private DcMotor backRightDrive = null;
    private DcMotor frontRightDrive = null;
    private DcMotor intake = null;
    private Servo servo = null;

    @Override
    public void runOpMode() {
        frontLeftDrive = hardwareMap.get(DcMotor.class, "frontLeft");
        backLeftDrive = hardwareMap.get(DcMotor.class, "backLeft");
        frontRightDrive = hardwareMap.get(DcMotor.class, "frontRight");
        backRightDrive = hardwareMap.get(DcMotor.class, "backRight");
        intake = hardwareMap.get(DcMotor.class, "intake");
        servo = hardwareMap.get(Servo.class, "servo");

        WebcamName webcam = hardwareMap.get(WebcamName.class, "Webcam 1");

        SectionColorProcessor colorProcessor = new SectionColorProcessor();

        VisionPortal visionPortal = new VisionPortal.Builder()
                .setCamera(webcam)
                .setCameraResolution(new Size(1280, 720))
                .addProcessor(colorProcessor)
                .build();

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {

            double frontLeftPower = 0;
            double frontRightPower = 0;
            double backLeftPower = 0;
            double backRightPower = 0;
            double intakePower = 0;

            intake.setPower(intakePower);
            frontLeftDrive.setPower(frontLeftPower);
            frontRightDrive.setPower(frontRightPower);
            backLeftDrive.setPower(backLeftPower);
            backRightDrive.setPower(backRightPower);

            telemetry.addData("power bl", backLeftPower);
            telemetry.addData("power fl", frontLeftPower);
            telemetry.addData("power br", backRightPower);
            telemetry.addData("power fr", frontRightPower);

            for (int i = 0; i < 25; i++) {
                telemetry.addData("Section " + i, colorProcessor.sectionColors[i]);
                if (colorProcessor.sectionColors[i].equals("BLUE") && i > 10 && i < 14){
                    backLeftPower = 0.5;
                    backRightPower = -0.5;
                    frontLeftPower = 0.5;
                    frontRightPower = -0.5;
                }
                else if (colorProcessor.sectionColors[i].equals("BLUE") && i < 10 && i > 14){
                    backLeftPower = -0.5;
                    backRightPower = 0.5;
                    frontLeftPower = -0.5;
                    frontRightPower = 0.5;
                } else {
                    backLeftPower = 0;
                    backRightPower = 0;
                    frontLeftPower = 0;
                    frontRightPower = 0;
                }

            }

            telemetry.update();
        }
        visionPortal.close();
    }

    public static class SectionColorProcessor implements VisionProcessor {
        public String[] sectionColors = new String[25];

        public SectionColorProcessor() {
            for (int i = 0; i < 25; i++) {
                sectionColors[i] = "UNKNOWN";
            }
        }

        @Override
        public void init(int width, int height, CameraCalibration calibration) {
        }

        @Override
        public Object processFrame(Mat frame, long captureTimeNanos) {
            int totalWidth = frame.width();
            int height = frame.height();
            int segmentWidth = totalWidth / 25;

            for (int i = 0; i < 25; i++) {
                int xStart = i * segmentWidth;
                int xEnd = (i == 24) ? totalWidth : (i + 1) * segmentWidth;

                Rect rect = new Rect(xStart, 0, xEnd - xStart, height);
                Mat submat = frame.submat(rect);

                Scalar mean = Core.mean(submat);
                submat.release();

                sectionColors[i] = classifyColor(mean.val[0], mean.val[1], mean.val[2]);

                Imgproc.rectangle(frame, rect, new Scalar(0, 255, 0), 2);
                Imgproc.putText(frame, String.valueOf(i), new Point(xStart + 5, 40),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);
            }

            return null;
        }

        @Override
        public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasToPixel, Object userContext) {
        }

        private String classifyColor(double r, double g, double b) {
            if (r < 50 && g < 50 && b < 50) return "BLACK";
            if (r > 200 && g > 200 && b > 200) return "WHITE";
            if (r > g + 40 && r > b + 40) return "RED";
            if (g > r + 40 && g > b + 40) return "GREEN";
            if (b > r + 40 && b > g + 40) return "BLUE";
            if (r > 150 && g > 150 && b < 100) return "YELLOW";
            return "OTHER";
        }
    }
}