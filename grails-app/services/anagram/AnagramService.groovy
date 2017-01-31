package anagram

import grails.transaction.Transactional
import redis.clients.jedis.Jedis

@Transactional 
class AnagramService {

    def redisService
    private static final int[] PRIMES = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113 ];

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

    // TODO, fix unit test for this
	def Map findAnagramsForWord(String word, String limitParam) {
		def key = generateKey(word)
		// get all set members for key
		def setMembers = redisService.smembers(key)
		def anagramMap = [:]
		
		if (setMembers.size() > 0) {
			def filteredMembers = setMembers.findAll {!it.contains(word)}

            if (limitParam != null) {
                int limit = Integer.parseInt(limitParam);
                def limitedMembers = (filteredMembers as List)[0..limit - 1]
                anagramMap.anagrams = limitedMembers
            }
            else {
                anagramMap.anagrams = filteredMembers
            }
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
