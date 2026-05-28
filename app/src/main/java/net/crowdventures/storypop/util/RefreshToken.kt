package net.crowdventures.storypop.util

import org.joda.time.DateTime

class RefreshToken (val token:String,val receivedDateTime: DateTime =DateTime.now())