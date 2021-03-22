/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.PHRED_Bot;


/**
 * This file contains an example of an iterative (Non-Linear) "OpMode".
 * An OpMode is a 'program' that runs in either the autonomous or the teleop period of an FTC match.
 * The names of OpModes appear on the menu of the FTC Driver Station.
 * When an selection is made from the menu, the corresponding OpMode
 * class is instantiated on the Robot Controller and executed.
 * <p>
 * This particular OpMode just executes a basic Tank Drive Teleop for a two wheeled robot
 * It includes all the skeletal structure that all iterative OpModes contain.
 * <p>
 * Use Android Studios to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list
 */

@TeleOp(name = "TeleOp: Iterative OpMode", group = "Iterative Opmode")

public class MainTeleOp extends OpMode {

    // Declare Constants

    // Declare OpMode members

    PHRED_Bot robot = new PHRED_Bot();

    private ElapsedTime runtime = new ElapsedTime();
    private ElapsedTime shootCycle = new ElapsedTime();

    private double rightFrontPower = 0.0;
    private double rightRearPower = 0.0;
    private double leftFrontPower = 0.0;
    private double leftRearPower = 0.0;

    // Debounce variables
    private boolean leftBumperPressed = false;
    private boolean rightBumperPressed = false;

    private enum State {
        DRIVE,
        DROP_LIFT,
        GRIP,
        RAISE_LIFT,
        DROP_RING,
        FLIPPER,
        SHOOT
    }

    private State currentState = State.DRIVE;

    @Override
    public void init() {
        telemetry.addData("Status", "Initialized");

        robot.initializeRobot();
        // Tell the driver that initialization is complete.

    }

    /*
     * Code to run REPEATEDLY after the driver hits INIT, but before they hit PLAY
     */
    @Override
    public void init_loop() {
         robot.liftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    /*
     * Code to run ONCE when the driver hits PLAY
     */
    @Override
    public void start() {
        runtime.reset();
        robot.liftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    /*
     * Code to run REPEATEDLY after the driver hits PLAY but before they hit STOP
     */
    @Override
    public void loop() {
        int liftEncoder;

        // Mechanum Mode use Left Stick for motion and Right Stick to rotate
        double drive = gamepad1.left_stick_y;
        double strafe = -gamepad1.left_stick_x;
        double turn = -gamepad1.right_stick_x;

        // - This uses basic math to combine motions and is easier to drive straight.
        leftFrontPower = clipPower(drive + turn + strafe);
        rightFrontPower = clipPower(-drive + turn + strafe);
        leftRearPower = clipPower(-drive + -turn + strafe);
        rightRearPower = clipPower(drive + -turn + strafe);

        // Now Drive the Robot
        robot.leftFrontDrive.setPower(leftFrontPower);
        robot.rightFrontDrive.setPower(rightFrontPower);
        robot.leftRearDrive.setPower(leftRearPower);
        robot.rightRearDrive.setPower(rightRearPower);

        // State Machine
        liftEncoder = robot.liftMotor.getCurrentPosition();

        // State Machine Actions: Grab, LiftUp, Drop, LiftDown, Shoot

        if(gamepad2.left_bumper != leftBumperPressed ) {
            if ( gamepad2.left_bumper  ) {
               // Flag trigger
               leftBumperPressed = true;
            } else {
                leftBumperPressed = false;
            }
        }

        if ( isStopped(drive, turn, strafe)) {
            if (gamepad2.right_bumper != rightBumperPressed) {
                if (gamepad2.right_bumper) {
                    // Shoot a ring
                    shootARing();
                    rightBumperPressed = true;
                } else {
                    rightBumperPressed = false;
                }
            }
        }

        // State Machine

        // Show the elapsed game time and wheel power.
        telemetry.addData("Status", "Run Time: " + runtime.toString());
        telemetry.addData("Front Motors", "left (%.2f), right (%.2f)", leftFrontPower, rightFrontPower);
        telemetry.addData("Lift Encoder", liftEncoder);
        telemetry.update();
    }


    /*
     * Code to run ONCE after the driver hits STOP
     */
    @Override
    public void stop() {
        robot.leftFrontDrive.setPower(0);
        robot.rightFrontDrive.setPower(0);
        robot.leftRearDrive.setPower(0);
        robot.rightRearDrive.setPower(0);
        robot.frontShooterMotor.setPower(0);
        robot.backShooterMotor.setPower(0);
        robot.liftMotor.setPower(0);

    }

    void shootARing() {
        // Turn Motors on
        robot.backShooterMotor.setPower(1.0);
        robot.frontShooterMotor.setPower(1.0);

        // set a timer
        shootCycle.reset();

        // start the servo
        robot.flipperServo.setPosition( robot.FLIPPER_FORWARD );

        // wait for the servo
        while ( robot.flipperServo.getPosition() < robot.FLIPPER_FORWARD) {
            // insert thumb twiddle

        }

        //pull the servo back
        robot.flipperServo.setPosition( robot.FLIPPER_BACK );

       //wait for the servo
        while (robot.flipperServo.getPosition() > robot.FLIPPER_BACK) {
            // insert thumb twiddle

        }

        // wait for the timer
        while ( shootCycle.milliseconds() < 1000 )  {
            // insert thumb twiddle
        }

        // return control

    }

    boolean isStopped(double drive, double turn, double strafe) {
        return drive == 0 && turn == 0 && strafe == 0;
    }

    double clipPower(double inputPower) {
        double outputPower = inputPower;
        if (outputPower > 1.0) {
            outputPower = 1.0;
        } else if (outputPower < -1.0 ) {
            outputPower = -1.0;
        }

        return outputPower;
    }

}
