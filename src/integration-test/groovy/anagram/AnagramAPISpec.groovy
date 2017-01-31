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
    def redisService

    def setup() {
		//redisService.flushAll()
    }

    def cleanup() {
		//redisService.flushAll()
    }

    void "Test adding words to the data store"() {
		redisService.flushAll()
        when:"we call a POST with JSON data to add words to our data store"
            def resp = restBuilder().post("$baseUrl/api/words") {
                contentType "application/json"
                json {
                    words = ["read", "dear", "dare"]
                }
            }

        then:"The response is correct"
            resp.status == 201
    }

    void "Test finding anagrams for the given word"() {
        given: "our sorted anagrams for the word read"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("dare")
            expectedAnagrams.add("dear")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/read")

        then:"The response is correct"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            Collections.sort(anagramList)
            expectedAnagrams.equals(anagramList)
    }

    void "Test finding anagrams for the given word with a limit passed in"() {
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/read?limit=1")

        then:"The response is correct"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            anagramList.size() == 1
    }


    RestBuilder restBuilder() {
        new RestBuilder()
    }
}
