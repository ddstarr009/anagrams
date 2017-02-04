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
    // if you change certain tests, e.g., the POST below, make sure you adjust the subsequent tests

    void "Test that returns a count of words in the corpus and min/max/median/average word length after dictionary loaded"() {
        when:"we make a GET req to the /words/stats endpoint after dictionary has been loaded"
            def resp = restBuilder().get("$baseUrl/api/v1/words/stats")

        then:"The resp is OK and the dictionary was loaded"
            resp.status == 200
            //235,885 is currently the size of the dictionary
            def wordCount = resp.json.wordCount
            def withoutCommas = wordCount.replaceAll(",", "")
            int intCount = Integer.parseInt(withoutCommas)
            intCount >= 235885
            
    }

    void "Test adding words to the data store"() {
		redisService.flushDB() // we want to flush everything before the test suite begins
        when:"we call a POST with JSON data to add words to our data store"
            def resp = restBuilder().post("$baseUrl/api/v1/words") {
                contentType "application/json"
                json {
                    words = ["Ader", "read", "dear", "dare", "listen", "silent"]
                }
            }

        then:"The words are added to the DB and we receive a 201 for created"
            resp.status == 201
    }

    void "Test that returns words with the most anagrams"() {
        when:"we make a GET req to the anagrams/most endpoint"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/most")

        then:"The resp is OK and the returned anagrams are correct and from the read group"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.wordsWithMostAnagrams)
            def list = anagramList.findAll { it == 'Ader' }
            list.size() == 1
            resp.json.countOfAnagramGroup == 4
    }

    void "Test that returns a count of words in the corpus and min/max/median/average word length"() {
        when:"we make a GET req to the /words/stats endpoint"
            def resp = restBuilder().get("$baseUrl/api/v1/words/stats")

        then:"The resp is OK and the returned stats are correct"
            resp.status == 200
            resp.json.wordCount == "6"
            resp.json.averageWordLength == "4.6666666667"
            resp.json.minimumWordLength == 4
            resp.json.maximumWordLength == 6
            resp.json.medianWordLength == 4
    }

    void "Test finding anagrams for the given word with no proper nouns in the response"() {
        when:"we call a GET with a specific token and specify we do not want proper nouns"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read?proper=false")

        then:"The resp is OK and the size is only 1"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            def list = anagramList.findAll { it == 'Ader' }
            list.size() == 0
    }

    // adding this to ensure silent exists before deleting it
    void "Test finding anagrams for the given word"() {
        given: "our expected anagram for the word listen"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("silent")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/listen")

        then:"The json includes the expected anagram defined above"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            expectedAnagrams.equals(anagramList)
    }

    void "Test deleting a word and its anagrams"() {
        when:"we call a DELETE with a specific token"
            def resp = restBuilder().delete("$baseUrl/api/v1/anagrams/listen")

        then:"The response is a 204 and the anagram family will be deleted"
            resp.status == 204
    }

    void "Test to verify that the listen anagram family has been deleted in previous test"() {
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/silent")

        then:"the returned json will be empty"
            resp.status == 200
            resp.json.isEmpty() == true
    }

    void "Test finding anagrams for the given word"() {
        given: "our sorted anagrams for the word read"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("Ader")
            expectedAnagrams.add("dare")
            expectedAnagrams.add("dear")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read")

        then:"The json includes the 2 expected anagrams defined above"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            Collections.sort(anagramList)
            expectedAnagrams.equals(anagramList)
    }

    void "Test finding anagrams for the given word with a limit passed in"() {
        when:"we call a GET with a specific token and limit and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read?limit=1")

        then:"The resp is OK and the size is only 1"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            anagramList.size() == 1
    }

    void "Test deleting a single word from data store"() {
        when:"we call a DELETE with a specific token"
            def resp = restBuilder().delete("$baseUrl/api/v1/words/dare")

        then:"The response is a 204"
            resp.status == 204
    }

    void "Test finding anagrams for the given word after deleting 'dare' in previous test"() {
        given: "our sorted anagrams for the word read"
            List expectedAnagrams = new ArrayList()
            expectedAnagrams.add("Ader")
            expectedAnagrams.add("dear")
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read")

        then:"the returned json will only have 'dear' in the results"
            resp.status == 200
            List anagramList = new ArrayList(resp.json.anagrams)
            expectedAnagrams.equals(anagramList)
    }

    void "Test deleting all words from data store"() {
        when:"we call a DELETE for all words"
            def resp = restBuilder().delete("$baseUrl/api/v1/words")

        then:"the status will be a 204"
            resp.status == 204
    }

    void "Test finding anagrams for a given word after deleting all words in the data store"() {
        when:"we call a GET with a specific token and find its anagrams"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/read")

        then:"the returned json will be empty"
            resp.status == 200
            resp.json.isEmpty() == true
    }

    void "Test GET that takes a set of words and returns whether or not they are all anagrams of each other"() {
        when:"we call a GET with a csv of words as a query param"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/checker?words=read,dare,dear")

        then:"the returned value should be true for read,dare,dear"
            resp.status == 200
            resp.text == "true"
    }

    void "Test GET that takes a set of words and returns whether or not they are all anagrams of each other, should be false"() {
        when:"we call a GET with a csv of words as a query param"
            def resp = restBuilder().get("$baseUrl/api/v1/anagrams/checker?words=read,dare,dear,fruit")

        then:"the returned value should be false for read,dare,dear,fruit"
            resp.status == 200
            resp.text == "false"
    }




    RestBuilder restBuilder() {
        new RestBuilder()
    }
}
