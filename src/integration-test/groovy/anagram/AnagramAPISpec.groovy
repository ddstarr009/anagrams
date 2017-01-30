package anagram


import grails.test.mixin.integration.Integration
import grails.transaction.*
import static grails.web.http.HttpHeaders.*
import static org.springframework.http.HttpStatus.*
import spock.lang.*
import geb.spock.*
import grails.plugins.rest.client.RestBuilder

@Integration
@Rollback
class AnagramAPISpec extends GebSpec {

    def setup() {
        // TODO
    }

    def cleanup() {
        // TODO, delete data store
    }

    void "Test adding words to the data store"() {
        when:"we call a POST with JSON data to add words to our data store"
            def resp = restBuilder().get("$baseUrl/words.json")

        then:"The response is correct"
            resp.status == 201
    }

    RestBuilder restBuilder() {
        new RestBuilder()
    }
}
