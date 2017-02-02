package anagram

import grails.transaction.Transactional
import redis.clients.jedis.Jedis

@Transactional 
class AnagramService {

    def redisService
    private static final int[] PRIMES = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113 ];

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
        redisService.srem(key, word)
    }

    def addToDataStore(List<String> wordsToAdd) {
        // multiple commands will only use a single connection instance by using withRedis
        redisService.withRedis { Jedis redis ->
            for (int i = 0 ; i < wordsToAdd.size() ; i++) {
                def word = wordsToAdd.get(i)
                def key = generateKey(word)
                redis.sadd(key, word)
            }
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
            members = (members as List)[0..limit - 1]
        }
        return members
    }

    // TODO, fix unit tests for this
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

    private String generateKey(String word) {
		def wordUpper = word.toUpperCase()
		char[] wordCharArray = wordUpper.toCharArray()
        // generating a unique key per anagram family, i.e., if a word has the same letters regardless of order, key will be the same
		long key = 1L;
        for (char c : wordCharArray) {
            if (c < 65) { // A in ascii is 65, anything less than 65 must be some special char/number
				throw new Exception("Please enter only valid alphabet chars.  No special chars please")
            }
            int pos = c - 65;
            key *= PRIMES[pos];
        }
        String strKey = String.valueOf(key)
        return strKey;
    }
}
