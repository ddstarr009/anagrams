package anagram

import grails.test.mixin.TestFor
import spock.lang.Specification
import grails.plugins.redis.RedisService

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(AnagramService)
class AnagramServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test areWordsInSameFamily method happy path"() {
		given: "a csv list of words"
			String words = "dear,read,dare"
		when: "anagramService.areWordsInSameFamily is called"
			boolean isSameFamily = service.areWordsInSameFamily(words)

		then: "Expect boolean to equal true for dear,read,dare"
			isSameFamily == true
    }

    void "test areWordsInSameFamily method, not in same family"() {
		given: "a csv list of words that are not in same family"
			String words = "dear,read,dare,silent"
		when: "anagramService.areWordsInSameFamily is called"
			boolean isSameFamily = service.areWordsInSameFamily(words)

		then: "Expect boolean to equal false for dear,read,dare,silent"
			isSameFamily == false
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

		then: "Expect an exception to be thrown"
			thrown(Exception)
    }

    void "test filterMembers service method with limit"() {
		given: "test data including a limit"
            Set testSet = new HashSet()
            testSet.add("read")
            testSet.add("dear")
		when: "anagramService.filterMembers is called"
			def returnedSet = service.filterMembers(testSet, "1", null)

		then: "Expect returned set size to be 1"
			returnedSet.size() == 1
    }

    void "test filterMembers service method with Proper noun restriction"() {
		given: "test data including a proper noun, false param"
            Set testSet = new HashSet()
            testSet.add("read")
            testSet.add("dear")
            testSet.add("Ader")
		when: "anagramService.filterMembers is called"
			def returnedSet = service.filterMembers(testSet, null, "false")

		then: "Expect size to be 2 and Ader to not be present"
			returnedSet.size() == 2
            def shouldBeEmptySet = returnedSet.findAll { it == 'Ader' }
            shouldBeEmptySet.size() == 0
    }

    void "test filterMembers service method with Proper noun restriction and limit"() {
		given: "test data including a proper noun, false param, and a limit"
            Set testSet = new HashSet()
            testSet.add("read")
            testSet.add("dear")
            testSet.add("Ader")
		when: "anagramService.filterMembers is called"
			def returnedSet = service.filterMembers(testSet, "1", "false")

		then: "Expect size to be 2 with no Ader"
			returnedSet.size() == 1
            def shouldBeEmptySet = returnedSet.findAll { it == 'Ader' }
            shouldBeEmptySet.size() == 0
    }

    void "test findAnagramsForWord happy path"() {
        given: "a test word and a mocked service"
            String word = "read"
            Set mockSet = new HashSet()
            mockSet.add("read")
            mockSet.add("dare")
            mockSet.add("dear")
            RedisService mockRedisService = GroovyMock()
            mockRedisService.smembers(_) >> mockSet
            service.redisService = mockRedisService
        when: "anagramService.findAnagramsForWord is called"
            def anagramMap = service.findAnagramsForWord(word, null, null)

        then: "Expect size to be 2 after removing read from the Set"
            anagramMap.anagrams
            anagramMap.anagrams.size() == 2
    }
}
