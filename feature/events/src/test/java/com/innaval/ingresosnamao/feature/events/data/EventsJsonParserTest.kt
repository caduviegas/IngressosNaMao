package com.innaval.ingresosnamao.feature.events.data

import com.innaval.ingresosnamao.feature.events.domain.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EventsJsonParserTest {
    private val parser = EventsJsonParser()

    @Test fun givenEventJsonWhenParsedThenPreservesPricesAndOrder() {
        val result = parser.parse(
            """[
                {"sku":1,"imageUrl":"https://example.com/1.jpg","name":" Evento 1 ","price":"125.00","quantity":120,"extra":true},
                {"sku":2,"imageUrl":"https://example.com/2.jpg","name":"Evento 2","price":0.1,"quantity":0},
                {"sku":3,"imageUrl":"https://example.com/3.jpg","name":"Evento 3","price":92233720368547758.07,"quantity":1}
            ]"""
        )
        assertEquals(
            listOf(
                Event("1", "https://example.com/1.jpg", "Evento 1", 12500, 120),
                Event("2", "https://example.com/2.jpg", "Evento 2", 10, 0),
                Event("3", "https://example.com/3.jpg", "Evento 3", Long.MAX_VALUE, 1),
            ), result
        )
    }

    @Test fun givenEmptyListAndTrailingZerosWhenParsedThenAcceptsBoth() {
        assertEquals(emptyList<Event>(), parser.parse("[]"))
        val json = """[{"sku":1,"imageUrl":"https://example.com","name":"N","price":1.230,"quantity":1}]"""
        assertEquals(123L, parser.parse(json).single().priceCents)
    }

    @Test fun givenRemoteConfigJsonWhenParsedThenAcceptsIntegerSkuAndStringPrices() {
        val json = """[
            {"sku":101,"imageUrl":"https://images.unsplash.com/a","name":"Festival","price":"49.90","quantity":120},
            {"sku":102,"imageUrl":"https://images.unsplash.com/b","name":"Samba","price":"35.00","quantity":80},
            {"sku":103,"imageUrl":"https://images.unsplash.com/c","name":"Teatro","price":"28.50","quantity":60},
            {"sku":104,"imageUrl":"https://images.unsplash.com/d","name":"Comedy","price":"39.90","quantity":150}
        ]"""
        val events = parser.parse(json)
        assertEquals(listOf("101", "102", "103", "104"), events.map { it.id })
        assertEquals(listOf(4990L, 3500L, 2850L, 3990L), events.map { it.priceCents })
        assertEquals(listOf(120, 80, 60, 150), events.map { it.quantity })
    }

    @Test fun givenInvalidContractWhenParsedThenRejectsEntireList() {
        val valid = """{"sku":1,"imageUrl":"https://example.com","name":"N","price":1,"quantity":1}"""
        val invalidItems = listOf(
            """{"sku":2,"imageUrl":"https://example.com","name":"N","price":"1.001","quantity":1}""",
            """{"sku":2,"imageUrl":"https://example.com","name":"N","price":-1,"quantity":1}""",
            """{"sku":2,"imageUrl":"https://example.com","name":"N","price":92233720368547758.08,"quantity":1}""",
            """{"sku":2,"imageUrl":"http://example.com","name":"N","price":1,"quantity":1}""",
            """{"sku":2,"imageUrl":"https://bad url","name":"N","price":1,"quantity":1}""",
            """{"sku":2,"imageUrl":"https://example.com","name":" ","price":1,"quantity":1}""",
            """{"sku":1,"imageUrl":"https://example.com","name":"N","price":1,"quantity":1}""",
            """{"sku":"2","imageUrl":"https://example.com","name":"N","price":1,"quantity":1}""",
            """{"sku":2,"imageUrl":"https://example.com","name":"N","price":1,"quantity":-1}""",
            """{"sku":2,"imageUrl":"https://example.com","name":"N","price":1,"quantity":1.5}""",
            """{"sku":2,"imageUrl":"https://example.com","name":"N","price":1}""",
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
