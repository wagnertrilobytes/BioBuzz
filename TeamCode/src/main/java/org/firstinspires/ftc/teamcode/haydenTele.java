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
    private DcMotor outtake1 = null;
    private DcMotor outtake2 = null;
    private DcMotor midtake = null;

    @Override
    public void runOpMode() {
        frontLeftDrive = hardwareMap.get(DcMotor.class, "frontLeftDrive");
        backLeftDrive = hardwareMap.get(DcMotor.class, "backLeftDrive");
        frontRightDrive = hardwareMap.get(DcMotor.class, "frontRightDrive");
        backRightDrive = hardwareMap.get(DcMotor.class, "backRightDrive");
        intake = hardwareMap.get(DcMotor.class, "intake");
        outtake1 = hardwareMap.get(DcMotor.class, "outtake1");
        outtake2 = hardwareMap.get(DcMotor.class, "outtake2");
        midtake = hardwareMap.get(DcMotor.class, "midtake");


        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);

        telemetry.addData("Status", "Initialized");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {

            double max;
            double co;
            double intakePower;
            double forward   = -gamepad1.left_stick_y;
            double strafe =  gamepad1.left_stick_x;
            double yaw     =  gamepad1.right_stick_x;
            double frontLeftPower  = (forward + (strafe * 1) + (yaw * 1.1));
            double frontRightPower = (forward - (strafe * 1) - (yaw * 1.1));
            double backLeftPower   = (forward - (strafe * 1) + (yaw * 1.1));
            double backRightPower  = (forward + (strafe * 1) - (yaw * 1.1));
            if (!(gamepad2.right_bumper)) {
                intakePower = gamepad2.right_trigger * -1;
            } else {
                intakePower = 1;
            }
            double outtake1power = gamepad2.left_trigger * 1;
            double outtake2power = gamepad2.left_trigger * -1;

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
            if (gamepad2.left_bumper){
                midtake.setPower(-1);
            } else {
                midtake.setPower(0);
            }

            intake.setPower(intakePower);
            frontLeftDrive.setPower(frontLeftPower * co);
            frontRightDrive.setPower(frontRightPower * co);
            backLeftDrive.setPower(backLeftPower * co);
            backRightDrive.setPower(backRightPower * co);
            outtake1.setPower(outtake1power);
            outtake2.setPower(outtake2power);
            telemetry.addData("power bl",backLeftPower);
            telemetry.addData("power fl",frontLeftPower);
            telemetry.addData("power br",backRightPower);
            telemetry.addData("power fr",frontRightPower);


            telemetry.update();
        }
    }
}
