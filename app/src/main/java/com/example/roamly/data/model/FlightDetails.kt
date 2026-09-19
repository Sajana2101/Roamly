package com.example.roamly.data.model

data class FlightDetails(
    val airline: String,
    val airportRoute: String,
    val flightDate: String,
    val boardingTime: String,
    val departureTime: String,
    val arrivalTime: String,
    val terminal: String = "",
    val boardingGate: String = ""
)