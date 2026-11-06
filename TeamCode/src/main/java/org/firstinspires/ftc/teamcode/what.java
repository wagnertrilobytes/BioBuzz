package org.firstinspires.ftc.teamcode;
import android.util.Size;


import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp(name="Boy you want working odometry pod", group="Linear OpMode")
public class what extends LinearOpMode {

    private DcMotor lf = null;
    private DcMotor lr = null;
    private DcMotor rf = null;
    private DcMotor rr = null;


    @Override
    public void runOpMode() {
        lf = hardwareMap.get(DcMotor.class, "lf");
        lr = hardwareMap.get(DcMotor.class, "lr");
        rf = hardwareMap.get(DcMotor.class, "rf");
        rr = hardwareMap.get(DcMotor.class, "rr");


        lf.setDirection(DcMotor.Direction.REVERSE);
        lr.setDirection(DcMotor.Direction.REVERSE);
        rf.setDirection(DcMotor.Direction.REVERSE);
        rr.setDirection(DcMotor.Direction.REVERSE);

        telemetry.addData("Status", "Initialized");


        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            telemetry.addData("Femboys lf",lf.getCurrentPosition());
            telemetry.addData("Femboys lr",lr.getCurrentPosition());
            telemetry.addData("Femboys rf",rf.getCurrentPosition());
            telemetry.addData("Femboys rr",rr.getCurrentPosition());

            telemetry.update();
        }
    }
}
