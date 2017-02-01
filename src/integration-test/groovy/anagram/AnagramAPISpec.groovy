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
    }

    def cleanup() {
    }

    // NOTE: the order of this test suite matters.  We are verifying Redis operations and app flow with this series of tests

    void "Test adding words to the data store"() {
		redisService.flushDB() // we want to flush everything before the test suite begins
        when:"we call a POST with JSON data to add words to our data store"
            def resp = restBuilder().post("$baseUrl/api/words") {
                contentType "application/json"
                json {
                    words = ["read", "dear", "dare"]
                }
            }

        then:"The words are added to the DB and we receive a 201 for created"
            resp.status == 201
    }

    void "Test finding anagrams for the given word"() {
        given: "our sorted anagrams for the word read"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("dare")
            expectedAnagrams.add("dear")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/anagrams/read")

        then:"The json includes the 2 expected anagrams defined above"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            Collections.sort(anagramList)
            expectedAnagrams.equals(anagramList)
    }

    void "Test finding anagrams for the given word with a limit passed in"() {
        when:"we call a GET with a specific token and limit and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/anagrams/read?limit=1")

        then:"The resp is OK and the size is only 1"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            anagramList.size() == 1
    }

    void "Test deleting a single word from data store"() {
        when:"we call a DELETE with a specific token"
            def resp = restBuilder().delete("$baseUrl/api/words/dare")

        then:"The response is a 204"
            resp.status == 204
    }

    void "Test finding anagrams for the given word after deleting 'dare' in previous test"() {
        given: "our sorted anagrams for the word read"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("dear")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/anagrams/read")

        then:"the returned json will only have 'dear' in the results"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            expectedAnagrams.equals(anagramList)
    }

    void "Test deleting all words from data store"() {
        when:"we call a DELETE for all words"
            def resp = restBuilder().delete("$baseUrl/api/words")

        then:"the status will be a 204"
            resp.status == 204
    }

    void "Test finding anagrams for a given word after deleting all words in the data store"() {
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/anagrams/read")

        then:"the returned json will be empty"
            resp.status == 200
            resp.json.isEmpty() == true
    }



    RestBuilder restBuilder() {
        new RestBuilder()
    }
}
