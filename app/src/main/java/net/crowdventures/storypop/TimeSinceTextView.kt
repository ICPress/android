package net.crowdventures.storypop

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import net.crowdventures.storypop.util.ViewModelUtil
import org.joda.time.DateTime

class TimeSinceTextView: androidx.appcompat.widget.AppCompatTextView {
    private var _timestamp :DateTime? = null
    public var timestamp:DateTime?
        get() = _timestamp
        set(value) {
            _timestamp = value
            continueTimeout = true
            val time = _timestamp?.toDateTime()?.millis?:return
            text = ViewModelUtil.getTimeAgo(time,_timestamp?:return)
            minuteCountdownTimer.start()
        }

    @Volatile var continueTimeout = false

    /*
        Just the constructors to create a new TextView...
     */
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    private val minuteCountdownTimer = object : CountDownTimer(ViewModelUtil.MINUTE_MILLIS,ViewModelUtil.MINUTE_MILLIS) {

        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            val time = _timestamp?.toDateTime()?.millis?:return
            text = ViewModelUtil.getTimeAgo(time,_timestamp?:return)
            if (continueTimeout) this.start()
        }
    }

    /*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        continueTimeout = false
    }

}