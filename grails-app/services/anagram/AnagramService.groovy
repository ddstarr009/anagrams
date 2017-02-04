package anagram

import grails.transaction.Transactional
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction
import java.text.NumberFormat

@Transactional 
class AnagramService {

    def redisService
    private static final int[] PRIMES = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113 ]
    private static final String ALL_WORDS_KEY = "allWords"
    private static final String WORD_AVG_KEY = "wordAvg"
    private static final String FAMILY_COUNT_KEY = "familyCount"

    // TODO, call on post and delete? unit  and integration test
    def fetchMostAnagrams() {
        // fetching words with most anagrams, which means we have to find the anagramGroupKey that has the largest count
        Map familyCount = redisService.hgetAll(FAMILY_COUNT_KEY)

        if (familyCount.isEmpty()) {
            return familyCount
        }

        def keyArray = familyCount.keySet() as String[]
        def groupKeyWithMost = keyArray[0]
        Map groupMapWithMost = [:]
        int initialMost = Integer.parseInt(familyCount[groupKeyWithMost])
        groupMapWithMost["length"] = initialMost
        groupMapWithMost["groupKey"] = groupKeyWithMost

        familyCount.each { k, v -> 
            int groupCount = Integer.parseInt(v)
            if (groupCount > groupMapWithMost.length) {
                groupMapWithMost.length = groupCount
                groupMapWithMost.groupKey = k
            }
        }

		def setMembers = redisService.smembers(groupMapWithMost.groupKey)
        Map result = [:]
        result.wordsWithMostAnagrams = setMembers
        result.countOfAnagramGroup = setMembers.size()
        return result
    }

    def areWordsInSameFamily(String words) {
        String[] wordsArray = words.split(",")
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
        def anagramGroupKey = generateKey(word)
        def elemScore = redisService.zscore(ALL_WORDS_KEY, word)
        def fieldValue = redisService.hget(FAMILY_COUNT_KEY, anagramGroupKey)

        if (elemScore != null) {
            // if in this block, it means that we do need to remove this word
    
            // we want to maintain atomicity to help keep multiple redis keys in sync
            redisService.withTransaction { Transaction transx ->
                // need to remove from both the sorted set(ALL_WORDS_KEY) and the specified anagramGroupKey set
                transx.zrem(ALL_WORDS_KEY, word)
                transx.srem(anagramGroupKey, word)

                // need to decrement count in FAMILY_COUNT_KEY hash for the word's anagram group
                if (fieldValue != null) { // should never really be null b/c of Transactions when adding/deleting words
                    long countValue = Long.parseLong(fieldValue)
                    if (countValue > 0) {
                        countValue--
                        transx.hset(FAMILY_COUNT_KEY, anagramGroupKey, Long.toString(countValue))
                    }
                }
            }
        }

        def wordAvg = calculateWordAvg()
        redisService.set(WORD_AVG_KEY, wordAvg.toString())
    }

    def fetchWordsStats() {
        def statsMap = [:]

        redisService.withRedis { Jedis redis ->
            def wordCount = redis.zcard(ALL_WORDS_KEY)
            statsMap.wordCount = NumberFormat.getNumberInstance(Locale.US).format(wordCount)

            def wordAvg = redis.get(WORD_AVG_KEY)

            if (wordAvg == null) {
                // key was not set for some reason, call calculateWordAvg and set for future reqs
                wordAvg = calculateWordAvg()
                redis.set(WORD_AVG_KEY, wordAvg.toString())
            }

            statsMap.averageWordLength = redis.get(WORD_AVG_KEY)

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
        for (int i = 0 ; i < wordsToAdd.size() ; i++) {
            def word = wordsToAdd.get(i)
            def anagramGroupKey = generateKey(word)
            def elemScore = redisService.zscore(ALL_WORDS_KEY, word)
            def fieldValue = redisService.hget(FAMILY_COUNT_KEY, anagramGroupKey)

            if (elemScore == null) {
                // b/c of null, we know that this word has not been added yet
                // we want to maintain atomicity to help keep multiple redis keys in sync
                redisService.withTransaction { Transaction transx ->
                    // adding duplicate data via zadd and sadd to redis for performance reasons
                    transx.zadd(ALL_WORDS_KEY, word.length(), word)
                    transx.sadd(anagramGroupKey, word)

                    // for each word that was just added, we are going to set or increment a hash in redis per anagram family
                    // this is for easier access to how many words there are per anagram group

                    if (fieldValue == null) { 
                        // we know that this anagramGroupKey doesn't exist yet
                        transx.hset(FAMILY_COUNT_KEY, anagramGroupKey, "1")
                    }
                    else {
                        // anagramGroupKey already existed in hash, need to increment 
                        long countValue = Long.parseLong(fieldValue)
                        countValue++
                        transx.hset(FAMILY_COUNT_KEY, anagramGroupKey, Long.toString(countValue))
                    }
                }
            }
        }
        def wordAvg = calculateWordAvg()
        redisService.set(WORD_AVG_KEY, wordAvg.toString())
    }

	def Map findAnagramsForWord(String word, String limitParam, String properParam) {
		def anagramGroupKey = generateKey(word)
		// get all set members for key
		def setMembers = redisService.smembers(anagramGroupKey)
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
            int limit = Integer.parseInt(limitParam)
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
		long key = 1L
        for (char c : wordCharArray) {
            if (c < 65 || c > 90) { // A in ascii is 65, anything less than 65 must be some special char/number
				throw new Exception("Please enter only valid alphabet chars.  No special chars please, you entered: ${c}")
            }
            int pos = c - 65
            key *= PRIMES[pos]
        }
        String strKey = String.valueOf(key)
        return strKey
    }

    private BigDecimal calculateWordAvg() {
        // calculate avg word length and store in redis
        def allWords = redisService.zrange(ALL_WORDS_KEY, 0 , -1)
        def allWordsSize = allWords.size()
        def wordLengthSum = 0

        for (String word : allWords) {
            wordLengthSum += word.length()
        }

        return wordLengthSum.div(allWordsSize)
    }

}
