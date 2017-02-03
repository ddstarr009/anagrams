package anagram

import grails.transaction.Transactional
import redis.clients.jedis.Jedis
import java.text.NumberFormat

@Transactional 
class AnagramService {

    def redisService
    private static final int[] PRIMES = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113 ];
    private static final String ALL_WORDS_KEY = "allWords"
    private static final String WORD_AVG_KEY = "wordAvg"

    def areWordsInSameFamily(String words) {
        String[] wordsArray = words.split(",");
        def currentKey = null
        boolean sameFamily = true

        for (int i = 0 ; i < wordsArray.length ; i++) {
            def key = generateKey(wordsArray[i])
            if (currentKey == null) {
                currentKey = key
            }
            else {
                if (currentKey != key) {
                    sameFamily = false
                    break
                }
            }
        }
        return sameFamily
    }

    def deleteAnagramFamily(String word) {
		def key = generateKey(word)
        redisService.del(key)
    }

    def deleteAllWords() {
        redisService.flushDB()
    }

    def deleteWord(String word) {
		def key = generateKey(word)
        // need to remove from both the sorted set and the specified key's set
        redisService.srem(key, word)
        redisService.zrem(ALL_WORDS_KEY, word.length(), word)
        calculateAndSetWordAvg()
    }

    // TODO, unit and integration test
    def fetchWordsStats() {
        def statsMap = [:]

        redisService.withRedis { Jedis redis ->
            statsMap.averageWordLength = redis.get(WORD_AVG_KEY)
            def wordCount = redis.zcard(ALL_WORDS_KEY)
            statsMap.wordCount = NumberFormat.getNumberInstance(Locale.US).format(wordCount)

            def smallestWordSet = redis.zrange(ALL_WORDS_KEY, 0,0)
            def smallestWord = smallestWordSet.iterator().next()
            statsMap.minimumWordLength = smallestWord.length()

            def largestWordSet = redis.zrange(ALL_WORDS_KEY, -1,-1)
            def largestWord = largestWordSet.iterator().next()
            statsMap.maximumWordLength = largestWord.length()

            long medianIndex = Math.floor(wordCount.div(2))
            def medianWordSet = redis.zrange(ALL_WORDS_KEY, medianIndex, medianIndex)
            def medianWord = medianWordSet.iterator().next()
            statsMap.medianWordLength = medianWord.length()
        }

        return statsMap
    }

    def addToDataStore(List<String> wordsToAdd) {
        // multiple commands will only use a single connection instance by using withRedis
        redisService.withRedis { Jedis redis ->
            for (int i = 0 ; i < wordsToAdd.size() ; i++) {
                def word = wordsToAdd.get(i)
                // adding duplicate data via zadd and sadd to redis for performance reasons
                redis.zadd(ALL_WORDS_KEY, word.length(), word)
                def key = generateKey(word)
                redis.sadd(key, word)
            }
        }
        calculateAndSetWordAvg()
    }

	def Map findAnagramsForWord(String word, String limitParam, String properParam) {
		def key = generateKey(word)
		// get all set members for key
		def setMembers = redisService.smembers(key)
		def anagramMap = [:]
		
		if (setMembers.size() > 0) {
            setMembers.remove(word)
            def filteredMembers = filterMembers(setMembers, limitParam, properParam)
            anagramMap.anagrams = filteredMembers

			return anagramMap
		} 
		else {
			return anagramMap
		}
	}

    private Set<String> filterMembers(Set<String> members, limitParam, properParam) {
        if (limitParam == null && properParam == null) {
            return members
        }

        if (properParam == "false") {
            members = members.findAll { !Character.isUpperCase(it.charAt(0)) }
        }

        if (limitParam != null) {
            int limit = Integer.parseInt(limitParam);
            if (limit >= members.size()) {
                return members
            }
            members = (members as List)[0..limit - 1]
        }
        return members
    }

    private String generateKey(String word) {
		def wordUpper = word.toUpperCase()
		char[] wordCharArray = wordUpper.toCharArray()
        // generating a unique key per anagram family, i.e., if a word has the same letters regardless of order, key will be the same
		long key = 1L;
        for (char c : wordCharArray) {
            if (c < 65 || c > 90) { // A in ascii is 65, anything less than 65 must be some special char/number
				throw new Exception("Please enter only valid alphabet chars.  No special chars please, you entered: ${c}")
            }
            int pos = c - 65;
            key *= PRIMES[pos];
        }
        String strKey = String.valueOf(key)
        return strKey;
    }

    // TODO, unit test
    private void calculateAndSetWordAvg() {
        // calculate avg word length and store in redis
        def allWords = redisService.zrange(ALL_WORDS_KEY, 0 , -1)
        def allWordsSize = allWords.size()
        def wordLengthSum = 0

        for (String word : allWords) {
            wordLengthSum += word.length()
        }

        def avgWordLength = wordLengthSum.div(allWordsSize)
        redisService.set(WORD_AVG_KEY, avgWordLength.toString())
    }

}
