package lex.model

import software.amazon.awssdk.services.lexruntimev2.model.Message
import software.amazon.awssdk.services.lexruntimev2.model.SessionState

data class Response(
    var sessionState: SessionState,
    var messages: List<Message>? = null,
)
