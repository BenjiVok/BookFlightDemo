import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.google.gson.Gson
import lex.model.*
import model.Flight
import software.amazon.awssdk.services.lexruntimev2.model.*
import software.amazon.awssdk.services.lexruntimev2.model.DialogAction
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate

class Handler : RequestStreamHandler {

    private val flights = listOf(
        Flight("Budapest", "London", LocalDate.parse("2023-01-31")),
        Flight("Budapest", "Dubai", LocalDate.parse("2023-02-10")),
        Flight("Budapest", "London", LocalDate.parse("2023-03-10"))
    )

    private val unavailableCities = listOf("Wien", "Debrecen")

    companion object {
        private val gson = Gson()

        private inline fun <reified T> Gson.fromJson(inputStream: InputStream): T =
            InputStreamReader(inputStream).use { fromJson(it, T::class.java) }

        private fun Gson.toJson(value: Any, outputStream: OutputStream): Unit =
            OutputStreamWriter(outputStream).use { toJson(value, it) }
    }

    private fun closeResponse(intentState: IntentState, message: String, event: LexV2Event) = Response(
        sessionState = event.sessionState.toBuilder()
            .dialogAction(DialogAction.builder().type(DialogActionType.CLOSE).build())
            .intent(
                event.sessionState.intent().toBuilder()
                    .confirmationState(ConfirmationState.CONFIRMED)
                    .state(intentState).build()
            )
            .build(),
        messages = listOf(
            Message.builder().content(message)
                .contentType(MessageContentType.PLAIN_TEXT).build()
        ),
    )

    private fun elicitResponse(slotToElicit: String, message: String, event: LexV2Event) = Response(
        sessionState = event.sessionState.toBuilder()
            .dialogAction(DialogAction.builder().type(DialogActionType.ELICIT_SLOT).slotToElicit(slotToElicit).build())
            .intent(event.sessionState.intent().toBuilder().state(IntentState.IN_PROGRESS).build())
            .build(),
        listOf(Message.builder().content(message).contentType(MessageContentType.PLAIN_TEXT).build())
    )

    private fun handleEvent(event: LexV2Event): Response {
        val originValue = event.sessionState.intent().slots()["origin"]?.value()?.interpretedValue()
        val destinationValue = event.sessionState.intent().slots()["destination"]?.value()?.interpretedValue()
        val dateValue = event.sessionState.intent().slots()["date"]?.value()?.interpretedValue()

        if (unavailableCities.contains(destinationValue)) return elicitResponse(
            "destination",
            "Destination is not available at the moment. Please select a different one.",
            event
        )

        if (unavailableCities.contains(originValue)) return elicitResponse(
            "origin",
            "Origin is not available at the moment. Please select a different one.",
            event
        )

        val bookedFlight = flights.firstOrNull {
            it.origin == originValue &&
                    it.destination == destinationValue &&
                    it.date.isEqual(LocalDate.parse(dateValue))
        }

        if (bookedFlight == null) return closeResponse(
            IntentState.FAILED,
            "No viable flight with your parameters.",
            event
        )

        return closeResponse(
            IntentState.FULFILLED,
            "Successfully booked you event on $dateValue from $originValue to $destinationValue",
            event
        )
    }

    override fun handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context?) {
        val inputEvent: LexV2Event = gson.fromJson(inputStream)
        gson.toJson(handleEvent(inputEvent), outputStream)
    }
}
