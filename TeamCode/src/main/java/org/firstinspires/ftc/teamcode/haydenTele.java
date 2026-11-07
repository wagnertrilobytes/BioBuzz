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

import org.firstinspires.ftc.teamcode.launcher.LauncherVelocityControl;
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
    private final LauncherVelocityControl launcher = new LauncherVelocityControl();

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
        // Launcher motors keep the SDK default direction. Velocity mode, when the
        // gate allows it, uses the same command signs as the power lines below.
        launcher.bind(outtake1, outtake2);

        telemetry.addData("Status", "Initialized");
        launcher.addTelemetry(telemetry);
        telemetry.update();
        waitForStart();
        launcher.primeToggle(gamepad2.y);

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
            // Existing power-mode signs. Velocity mode uses the same +1 and -1 signs.
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
            launcher.command(gamepad2.left_trigger, gamepad2.y, outtake1power, outtake2power);
            telemetry.addData("power bl",backLeftPower);
            telemetry.addData("power fl",frontLeftPower);
            telemetry.addData("power br",backRightPower);
            telemetry.addData("power fr",frontRightPower);
            launcher.addTelemetry(telemetry);


            telemetry.update();
        }
        launcher.stopSafe();
    }
}
