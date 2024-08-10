package com.fallgan.customerapp.model

import java.io.Serializable

class TrackTimeLine : Serializable {
    lateinit var date: String
    lateinit var location: String
    lateinit var activity: String
}