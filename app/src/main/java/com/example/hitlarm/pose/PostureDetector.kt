package com.example.hitlarm.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class PostureDetector {

    enum class ExerciseState {
        UP,
        DOWN
    }

    private var currentState = ExerciseState.UP
    private var count = 0

    fun getCount(): Int = count
    fun resetCount() {
        count = 0
        currentState = ExerciseState.UP
    }

    fun processPose(pose: Pose, isPushUp: Boolean): Pair<Int, String> {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            return Pair(count, "No pose detected. Step in front of the camera.")
        }

        if (isPushUp) {
            // Pushup: Left or Right elbow angle
            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
            val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
            val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

            val leftConfidence = if (leftShoulder != null && leftElbow != null && leftWrist != null) {
                minOf(leftShoulder.inFrameLikelihood, leftElbow.inFrameLikelihood, leftWrist.inFrameLikelihood)
            } else 0f

            val rightConfidence = if (rightShoulder != null && rightElbow != null && rightWrist != null) {
                minOf(rightShoulder.inFrameLikelihood, rightElbow.inFrameLikelihood, rightWrist.inFrameLikelihood)
            } else 0f

            // Use the arm with higher confidence
            val angle = if (leftConfidence > 0.5f && leftConfidence >= rightConfidence) {
                getAngle(leftShoulder!!, leftElbow!!, leftWrist!!)
            } else if (rightConfidence > 0.5f) {
                getAngle(rightShoulder!!, rightElbow!!, rightWrist!!)
            } else {
                return Pair(count, "Make sure your shoulders, elbows, and wrists are visible.")
            }

            // State machine
            when (currentState) {
                ExerciseState.UP -> {
                    if (angle < 95.0) {
                        currentState = ExerciseState.DOWN
                    }
                    return Pair(count, "Go Down!")
                }
                ExerciseState.DOWN -> {
                    if (angle > 155.0) {
                        currentState = ExerciseState.UP
                        count++
                    }
                    return Pair(count, "Push Up!")
                }
            }
        } else {
            // Squat: Left or Right knee angle
            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
            val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

            val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
            val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

            val leftConfidence = if (leftHip != null && leftKnee != null && leftAnkle != null) {
                minOf(leftHip.inFrameLikelihood, leftKnee.inFrameLikelihood, leftAnkle.inFrameLikelihood)
            } else 0f

            val rightConfidence = if (rightHip != null && rightKnee != null && rightAnkle != null) {
                minOf(rightHip.inFrameLikelihood, rightKnee.inFrameLikelihood, rightAnkle.inFrameLikelihood)
            } else 0f

            // Use the leg with higher confidence
            val angle = if (leftConfidence > 0.5f && leftConfidence >= rightConfidence) {
                getAngle(leftHip!!, leftKnee!!, leftAnkle!!)
            } else if (rightConfidence > 0.5f) {
                getAngle(rightHip!!, rightKnee!!, rightAnkle!!)
            } else {
                return Pair(count, "Make sure your hips, knees, and ankles are visible.")
            }

            // State machine
            when (currentState) {
                ExerciseState.UP -> {
                    if (angle < 105.0) {
                        currentState = ExerciseState.DOWN
                    }
                    return Pair(count, "Squat Down!")
                }
                ExerciseState.DOWN -> {
                    if (angle > 165.0) {
                        currentState = ExerciseState.UP
                        count++
                    }
                    return Pair(count, "Stand Up!")
                }
            }
        }
    }

    private fun getAngle(first: PoseLandmark, middle: PoseLandmark, last: PoseLandmark): Double {
        var angle = Math.toDegrees(
            (atan2(last.position.y - middle.position.y, last.position.x - middle.position.x) -
                    atan2(first.position.y - middle.position.y, first.position.x - middle.position.x)).toDouble()
        )
        angle = abs(angle)
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle
    }
}
