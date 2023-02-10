package lex.model

import software.amazon.awssdk.services.lexruntimev2.model.SessionState

data class LexV2Event(
    var sessionState: SessionState,
)
