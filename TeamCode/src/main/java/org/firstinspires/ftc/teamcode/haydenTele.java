package org.firstinspires.ftc.teamcode;
import android.util.Size;


import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.opencv.ImageRegion;
import org.firstinspires.ftc.vision.opencv.PredominantColorProcessor;
@TeleOp(name="Boy you want hotdog / USE THIS TELEOP", group="Linear OpMode")
public class haydenTele extends LinearOpMode {

    private DcMotor frontLeftDrive = null;
    private DcMotor backLeftDrive = null;

    private DcMotor backRightDrive = null;
    private DcMotor frontRightDrive = null;

    private DcMotor intake = null;


    @Override
    public void runOpMode() {
        frontLeftDrive = hardwareMap.get(DcMotor.class, "frontLeft");
        backLeftDrive = hardwareMap.get(DcMotor.class, "backLeft");
        frontRightDrive = hardwareMap.get(DcMotor.class, "frontRight");
        backRightDrive = hardwareMap.get(DcMotor.class, "backRight");
        intake = hardwareMap.get(DcMotor.class, "intake");
        WebcamName webcam = hardwareMap.get(WebcamName.class, "Webcam 1");
        PredominantColorProcessor colorCamera =
                new PredominantColorProcessor.Builder()
                        .setRoi(ImageRegion.asUnityCenterCoordinates(
                                -0.20, 0.20, 0.20, -0.20))
                        .setSwatches(
                                PredominantColorProcessor.Swatch.RED,
                                PredominantColorProcessor.Swatch.BLUE,
                                PredominantColorProcessor.Swatch.YELLOW,
                                PredominantColorProcessor.Swatch.BLACK,
                                PredominantColorProcessor.Swatch.WHITE)
                        .build();

        VisionPortal visionPortal = new VisionPortal.Builder()
                .setCamera(webcam)
                .setCameraResolution(new Size(1280, 720))
                .addProcessor(colorCamera)
                .build();
        //servo = hardwareMap.get(Servo.class, "servo");

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);

        telemetry.addData("Status", "Initialized");
        telemetry.addData("FEM", "BOY");

        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {

            double max;
            double co;
            double forward   = -gamepad1.left_stick_y;
            double strafe =  gamepad1.left_stick_x;
            double yaw     =  gamepad1.right_stick_x;
            double frontLeftPower  = (forward + (strafe * -1) + (yaw * 1.1));
            double frontRightPower = (forward - (strafe * -1) - (yaw * 1.1));
            double backLeftPower   = (forward - (strafe * -1) + (yaw * 1.1));
            double backRightPower  = (forward + (strafe * -1) - (yaw * 1.1));
            double intakePower = gamepad1.right_trigger * -1;
            max = Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower));
            max = Math.max(max, Math.abs(backLeftPower));
            max = Math.max(max, Math.abs(backRightPower));

            if (max > 1.0) {
                frontLeftPower  /= max;
                frontRightPower /= max;
                backLeftPower   /= max;
                backRightPower  /= max;
            }
            if (gamepad1.a){
                co = 0.25;
            } else {
                co = 1;
            }
            intake.setPower(intakePower);
            frontLeftDrive.setPower(frontLeftPower * co);
            frontRightDrive.setPower(frontRightPower * co);
            backLeftDrive.setPower(backLeftPower * co);
            backRightDrive.setPower(backRightPower * co);
            telemetry.addData("power bl",backLeftPower);
            telemetry.addData("power fl",frontLeftPower);
            telemetry.addData("power br",backRightPower);
            telemetry.addData("power fr",frontRightPower);
            PredominantColorProcessor.Result result = colorCamera.getAnalysis();

            telemetry.addData("Camera color", result.closestSwatch);
            telemetry.addData("Camera RGB", "(%d, %d, %d)",
                    result.RGB[0], result.RGB[1], result.RGB[2]);
            telemetry.update();
        }
        visionPortal.close();
    }
}
