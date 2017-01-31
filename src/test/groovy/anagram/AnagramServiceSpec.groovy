package anagram

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(AnagramService)
class AnagramServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test generateKey method happy path"() {
		given: "a test word"
			String word = "dear"
		when: "anagramService.generateKey() is called"
			def key = service.generateKey(word)

		then: "Expect key to equal a certain product of prime numbers"
			key == "9394"
    }

    void "test generateKey method special chars should throw exception"() {
		given: "a weird test word"
			String word = "d*llea(r"
		when: "anagramService.generateKey() is called"
			def key = service.generateKey(word)

		then: "Expect key to equal a certain product of prime numbers"
			thrown(Exception)
    }

    void "test findAnagramsForWord happy path"() {
		given: "a test word"
			String word = "read"
            RedisService mockRedisService = Mock()
            service.redisService = mockRedisService
		when: "anagramService.findAnagramsForWord is called"
			def anagramMap = service.findAnagramsForWord(word, null)

		then: "Expect key to equal a certain product of prime numbers"
            def mockSet = ["read", "dare", "dear"] as Set
		    1 * mockRedisService.smembers(!null) >> mockSet
    }

/*    void "test findAnagramsForWord happy path with limit"() {*/
		//given: "a test word"
			//String word = "read"
		//when: "anagramService.findAnagramsForWord is called with limit arg"
			//def anagramMap = service.findAnagramsForWord(word, 1)

		//then: "Expect key to equal a certain product of prime numbers"
			//thrown(Exception)
    //}

}
