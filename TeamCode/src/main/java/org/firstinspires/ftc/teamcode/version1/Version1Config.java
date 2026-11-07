package org.firstinspires.ftc.teamcode.version1;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Editable settings for the Version 1 teleop.
 *
 * Review base is master dd23f69bc56a8b58a484ef89b42ba6dfd3ddd8b9
 * ("Update leftEdgeAuto.java", 2026-11-07). Commit 7cd0a6ce0a5f64c91cbf1ac416e6d863a836b85c
 * is an ancestor of that tip, not the tip itself.
 *
 * This repository has no Robot Controller configuration file. The strings and
 * directions below are therefore unset. Comments name strings that appear in
 * existing student files. Those comments are evidence from source, not a
 * measured hardware map and not values this class applies.
 *
 * Motion stays off until the matching confirmation boolean is true AND the
 * related names, directions, and powers are filled in with values the team
 * has checked. Restart the opmode after every edit. Editing this file does
 * not install code onto a robot.
 */
public class Version1Config {

    /**
     * True only after someone compared every name below with the active
     * Driver Station configuration, character for character.
     */
    public static boolean HARDWARE_MAP_CONFIRMED = false;

    /**
     * True only after each drive wheel was checked, robot supported, and
     * positive power on that motor drives that wheel forward.
     */
    public static boolean DRIVE_DIRECTIONS_CONFIRMED = false;

    /**
     * True only after each mechanism direction was checked so that positive
     * power is intake-in, feed-toward-launcher, and launch spin on each launcher.
     */
    public static boolean MECHANISM_DIRECTIONS_CONFIRMED = false;

    /**
     * True only after the team chose the power numbers below for a supported robot.
     * This file does not contain a measured safe power.
     */
    public static boolean POWER_LIMITS_CONFIRMED = false;

    /**
     * Drive names. Leave "" until the Driver Station configuration is read.
     *
     * Current haydenTele.java, leftEdgeAuto.java, and rightEdgeAuto.java call
     * these "frontLeftDrive", "backLeftDrive", "frontRightDrive", and "backRightDrive".
     * testop.java, testAuto.java, pedro/Constants.java, and haydenTele.java at
     * 7cd0a6ce call the drive motors "frontLeft", "backLeft", "frontRight", and
     * "backRight". Both families are in this repository. Neither is confirmed here.
     */
    public static String FRONT_LEFT_NAME = "";
    public static String BACK_LEFT_NAME = "";
    public static String FRONT_RIGHT_NAME = "";
    public static String BACK_RIGHT_NAME = "";

    /**
     * Intake name. The student files that name an intake use "intake".
     * That string is still not applied until you type it here.
     */
    public static String INTAKE_NAME = "";

    /**
     * Transfer / feed name. Current haydenTele.java, leftEdgeAuto.java, and
     * rightEdgeAuto.java call this motor "midtake". No other feed name is in
     * the current student opmodes. Type it only if the Driver Station uses it.
     */
    public static String FEED_NAME = "";

    /**
     * Both launcher motors. Current haydenTele.java, leftEdgeAuto.java, and
     * rightEdgeAuto.java call them "outtake1" and "outtake2". Those names are
     * historical source evidence of two launcher motors. They are not applied
     * until you type them here. This package does not use a single-flywheel,
     * arm, or servo launcher.
     */
    public static String LAUNCHER_1_NAME = "";
    public static String LAUNCHER_2_NAME = "";

    /**
     * Null means unset. An unset direction blocks that actuator.
     * Current haydenTele and both edge autos set the drive motors to
     * front-left REVERSE, back-left REVERSE, front-right FORWARD, back-right FORWARD.
     * Those files never call setDirection on intake, midtake, outtake1, or outtake2.
     * Do not copy a direction into the fields below until a supported test shows it.
     */
    public static DcMotor.Direction FRONT_LEFT_DIRECTION = null;
    public static DcMotor.Direction BACK_LEFT_DIRECTION = null;
    public static DcMotor.Direction FRONT_RIGHT_DIRECTION = null;
    public static DcMotor.Direction BACK_RIGHT_DIRECTION = null;
    public static DcMotor.Direction INTAKE_DIRECTION = null;
    public static DcMotor.Direction FEED_DIRECTION = null;
    public static DcMotor.Direction LAUNCHER_1_DIRECTION = null;
    public static DcMotor.Direction LAUNCHER_2_DIRECTION = null;

    /**
     * +1 or -1 after a supported strafe test. 0 blocks the whole drive.
     * +1 matches current haydenTele.java and the FTC sample mecanum mix.
     * -1 matches haydenTele.java at 7cd0a6ce and testop.java.
     * The two signs are both present in this repo, so the default is unset.
     */
    public static int STRAFE_SIGN = 0;

    /**
     * Multiplier on the rotate stick before normalization. 1.0 adds no extra gain.
     * Current haydenTele multiplies yaw by 1.1. That 1.1 is not copied here.
     */
    public static double YAW_GAIN = 1.0;

    /**
     * Stick and trigger noise floor. Values inside the band become 0, and the
     * rest of the travel is scaled so full deflection is still full command.
     * This is a software filter, not a measured robot speed.
     */
    public static double STICK_DEADBAND = 0.05;
    public static double TRIGGER_DEADBAND = 0.05;

    /**
     * Extra scale while gamepad 1 A is held. 0.25 matches the slow factor in
     * haydenTele.java. It is a stick scale, not a measured robot speed.
     * It has an effect only after drive is otherwise armed.
     */
    public static double SLOW_MODE_SCALE = 0.25;

    /**
     * 0.0 commands no motion for that actuator. A usable value is greater than 0
     * and at most 1. This file does not choose a tested power for the robot.
     * Current haydenTele commands several of these mechanisms at magnitude 1.
     */
    public static double DRIVE_POWER_SCALE = 0.0;
    public static double INTAKE_POWER = 0.0;
    public static double FEED_POWER = 0.0;
    public static double LAUNCHER_1_POWER = 0.0;
    public static double LAUNCHER_2_POWER = 0.0;

    /**
     * Milliseconds both launchers must be commanded before feed-toward-launcher
     * is allowed. -1 means the team has not chosen a hold, so feed toward the
     * launcher stays off. 0 means no extra wait. A positive number is only a
     * timer. There is no launcher speed sensor in this opmode, and this number
     * is not a measured ready speed.
     */
    public static long LAUNCHER_HOLD_BEFORE_FEED_MS = -1L;

    /**
     * Asked of every motor after it is found, including while motion is blocked.
     * setPower(0) plus BRAKE is the stop behavior. It is not a measured stopping
     * distance. Set this to FLOAT if the team decides the controller should coast.
     */
    public static DcMotor.ZeroPowerBehavior ZERO_POWER_BEHAVIOR = DcMotor.ZeroPowerBehavior.BRAKE;

    public static boolean powerInRange(double power) {
        return !Double.isNaN(power) && !Double.isInfinite(power) && power > 0.0 && power <= 1.0;
    }

    public static boolean deadbandInRange(double deadband) {
        return !Double.isNaN(deadband) && !Double.isInfinite(deadband) && deadband >= 0.0 && deadband < 0.5;
    }

    public static boolean yawGainInRange(double gain) {
        return !Double.isNaN(gain) && !Double.isInfinite(gain);
    }
}
