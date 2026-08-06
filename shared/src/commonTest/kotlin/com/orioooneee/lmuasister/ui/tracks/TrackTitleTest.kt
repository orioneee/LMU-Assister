package com.orioooneee.lmuasister.ui.tracks

import com.orioooneee.lmuasister.data.remote.TrackFullDto
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackTitleTest {

    @Test
    fun curatedDisplayNameWinsOverFullAndOfficialNames() {
        val track = TrackFullDto(
            id = "daytona",
            name = "Daytona International Speedway",
            displayName = "Daytona",
            fullName = "Daytona Road Course - IMSA",
        )

        assertEquals("Daytona", trackTitle(track))
    }
}
