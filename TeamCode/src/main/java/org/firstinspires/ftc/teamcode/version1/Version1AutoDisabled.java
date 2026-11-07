package org.firstinspires.ftc.teamcode.version1;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

/**
 * Disabled placeholder. This class commands no drive, intake, feed, or launcher motion.
 *
 * It contains no distances, headings, tick counts, or timers that move the robot.
 * Removing @Disabled is not a calibration. The loop still sends power 0.
 *
 * leftEdgeAuto.java, rightEdgeAuto.java, testAuto.java, and pedropathingAutoStart.java
 * are unchanged. They remain registered with the Driver Station. This class does not
 * disable them and does not make their heading, feed, or park behavior safe.
 */
@Disabled
@Autonomous(name = "V1 Auto DISABLED no motion", group = "Version1")
public class Version1AutoDisabled extends LinearOpMode {

    private final Version1Hardware robot = new Version1Hardware();

    @Override
    public void runOpMode() {
        try {
            robot.init(hardwareMap);
            while (!isStarted() && !isStopRequested()) {
                robot.zeroAll();
                telemetry.addData("Auto", "DISABLED placeholder");
                telemetry.addData("Motion", "none commanded");
                telemetry.addData("Old autos", "leftEdgeAuto, rightEdgeAuto, testAuto, pedropathingAutoStart unchanged");
                robot.addMapReport(telemetry);
                telemetry.update();
                idle();
            }
            while (opModeIsActive()) {
                robot.zeroAll();
                telemetry.addData("Auto", "loop is running and sending power 0");
                telemetry.addData("Motion", "none commanded");
                telemetry.addData("Old autos", "still present and not made safe by this class");
                telemetry.update();
            }
        } finally {
            robot.zeroAll();
        }
    }
}
