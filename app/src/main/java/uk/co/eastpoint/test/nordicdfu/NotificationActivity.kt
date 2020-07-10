package uk.co.eastpoint.test.nordicdfu

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Adapted from Nordic Library - this activity gets called from notification returned from DFU Service
 */
class NotificationActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If this activity is the root activity of the task, the app is not running

        // If this activity is the root activity of the task, the app is not running
        if (isTaskRoot) {
            // Start the app before finishing
//            val parentIntent = Intent(this, MainActivity::class.java)
//            parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val startAppIntent = Intent(this, dfuactivity::class.java)
            if (intent != null && intent.extras != null) startAppIntent.putExtras(
                intent.extras!!
            )
//            startActivities(arrayOf(parentIntent, startAppIntent))
            startActivity(startAppIntent)
        }

        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack

        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack
        finish()
    }
}