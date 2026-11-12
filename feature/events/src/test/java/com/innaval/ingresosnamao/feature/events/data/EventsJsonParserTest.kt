package com.innaval.ingresosnamao.feature.events.data

import com.innaval.ingresosnamao.feature.events.domain.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EventsJsonParserTest {
    private val parser = EventsJsonParser()

    @Test fun parsesExactPricesAndPreservesOrder() {
        val result = parser.parse(
            """[
                {"id":" 1 ","imageUrl":"https://example.com/1.jpg","name":" Evento 1 ","price":125.00,"extra":true},
                {"id":"2","imageUrl":"https://example.com/2.jpg","name":"Evento 2","price":0.1},
                {"id":"3","imageUrl":"https://example.com/3.jpg","name":"Evento 3","price":92233720368547758.07}
            ]"""
        )
        assertEquals(
            listOf(
                Event("1", "https://example.com/1.jpg", "Evento 1", 12500),
                Event("2", "https://example.com/2.jpg", "Evento 2", 10),
                Event("3", "https://example.com/3.jpg", "Evento 3", Long.MAX_VALUE),
            ), result
        )
    }

    @Test fun acceptsEmptyListAndTrailingDecimalZeros() {
        assertEquals(emptyList<Event>(), parser.parse("[]"))
        assertEquals(123L, parser.parse("""[{"id":"1","imageUrl":"https://example.com","name":"N","price":1.230}]""").single().priceCents)
    }

    @Test fun rejectsInvalidContractWithoutReturningPartialList() {
        val valid = """{"id":"1","imageUrl":"https://example.com","name":"N","price":1}"""
        val invalidItems = listOf(
            """{"id":"2","imageUrl":"https://example.com","name":"N","price":1.001}""",
            """{"id":"2","imageUrl":"https://example.com","name":"N","price":-1}""",
            """{"id":"2","imageUrl":"https://example.com","name":"N","price":"1.00"}""",
            """{"id":"2","imageUrl":"https://example.com","name":"N","price":92233720368547758.08}""",
            """{"id":"2","imageUrl":"http://example.com","name":"N","price":1}""",
            """{"id":"2","imageUrl":"https://bad url","name":"N","price":1}""",
            """{"id":"2","imageUrl":"https://example.com","name":" ","price":1}""",
            """{"id":" 1 ","imageUrl":"https://example.com","name":"N","price":1}""",
            """{"id":"2","imageUrl":"https://example.com","name":"N"}""",
            "null",
        )
        invalidItems.forEach { invalid ->
            assertThrows("Expected invalid item: $invalid", Exception::class.java) {
                parser.parse("[$valid,$invalid]")
            }
        }
        listOf("", "{}", "[", "null").forEach { invalid ->
            assertThrows(Exception::class.java) { parser.parse(invalid) }
        }
    }
}
