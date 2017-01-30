package anagram

import grails.transaction.Transactional
import redis.clients.jedis.Jedis

@Transactional 
class AnagramService {

    def redisService
    private static final int[] PRIMES = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113 ];

    def addToDataStore(List<String> wordsToAdd) {
        // multiple commands will only use a single connection instance by using withRedis
        redisService.withRedis { Jedis redis ->
            for (int i = 0 ; i < wordsToAdd.size() ; i++) {
                def word = wordsToAdd.get(i)
                def wordUpper = word.toUpperCase()
                char[] wordCharArray = wordUpper.toCharArray()
                def key = generateKey(wordCharArray)
                redis.sadd(key, word)
            }
        }
    }

    private String generateKey(char[] letters) {
        // generating a unique key per anagram family, i.e., if a word has the same letters regardless of order, key will be the same
		long key = 1L;
        for (char c : letters) {
            if (c < 65) { // A in ascii is 65, anything less than 65 must be some special char/number
                return -1; // TODO, maybe throw exception b/c some weird char was passed in as a word
            }
            int pos = c - 65;
            key *= PRIMES[pos];
        }
        String strKey = String.valueOf(key)
        return strKey;
    }
}
