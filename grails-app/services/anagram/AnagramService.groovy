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


    def fetchGroupsByMinSize(String minSize) {
        // we want to return anagram groups of size >= minSize
        Map anagramGroups = [:]

        redisService.withRedis { Jedis redis ->
            Set anagramGroupKeys = redis.zrangeByScore(FAMILY_COUNT_KEY, minSize, "+inf")

            def i = 1
            for (String anagramGroupKey : anagramGroupKeys) {
                Set group = redis.smembers(anagramGroupKey)
                anagramGroups["group" + i] = [size: group.size(), members: group]
                i++
            }
        }
        return anagramGroups
    }

    def fetchMostAnagrams() {
        // fetching words with most anagrams
        Map anagramMap = [:]

        redisService.withRedis { Jedis redis ->
            def anagramGroupKeySet = redis.zrange(FAMILY_COUNT_KEY, -1,-1)
            if (anagramGroupKeySet.size() > 0) {
                String anagramGroupKey = anagramGroupKeySet.iterator().next()
                def setMembers = redis.smembers(anagramGroupKey)
                anagramMap.wordsWithMostAnagrams = setMembers
                anagramMap.countOfAnagramGroup = setMembers.size()
                return anagramMap
            }
            else {
                return anagramMap
            }
        }
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
        def setMembers = redisService.smembers(key)

        for (String wordToDelete : setMembers) {
            deleteWord(wordToDelete)
        }
    }

    def deleteAllWords() {
        redisService.flushDB()
    }

    def deleteWord(String word) {
        def anagramGroupKey = generateKey(word)
        def elemScore = redisService.zscore(ALL_WORDS_KEY, word) // this is to check whether the word exists yet

        if (elemScore != null) {
            // if in this block, it means that we do need to remove this word

            // we want to maintain atomicity and keep multiple redis keys in sync
            redisService.withTransaction { Transaction transx ->
                // need to remove from both the sorted set(ALL_WORDS_KEY) and the specified anagramGroupKey set
                transx.zrem(ALL_WORDS_KEY, word)
                transx.srem(anagramGroupKey, word)
                transx.zincrby(FAMILY_COUNT_KEY, -1, anagramGroupKey)
            }

            // calculating word avg for fast search on future GETs
            try {
                def wordAvg = calculateWordAvg()
                redisService.set(WORD_AVG_KEY, wordAvg.toString())
            } catch (Exception ex) {
                log.error "Exception occurred. ${ex?.message}", ex
                // going to delete WORD_AVG_KEY to ensure data correctness
                redisService.del(WORD_AVG_KEY)
            }
        }
    }

    def fetchWordsStats() {
        def statsMap = [:]

        redisService.withRedis { Jedis redis ->
            def wordCount = redis.zcard(ALL_WORDS_KEY)
            // should i add commas? not sure, but i think i'll leave it
            statsMap.wordCount = NumberFormat.getNumberInstance(Locale.US).format(wordCount)

            def wordAvg = redis.get(WORD_AVG_KEY)

            if (wordAvg == null) {
                // key was not set for some reason, call calculateWordAvg and set for future reqs
                wordAvg = calculateWordAvg()
                redis.set(WORD_AVG_KEY, wordAvg.toString())
            }

            statsMap.averageWordLength = redis.get(WORD_AVG_KEY)

            def smallestWordSet = redis.zrange(ALL_WORDS_KEY, 0,0)
            if (smallestWordSet.size() > 0) {
                def smallestWord = smallestWordSet.iterator().next()
                statsMap.minimumWordLength = smallestWord.length()
            }
            else {
                statsMap.minimumWordLength = 0
            }

            def largestWordSet = redis.zrange(ALL_WORDS_KEY, -1,-1)
            if (largestWordSet.size() > 0) {
                def largestWord = largestWordSet.iterator().next()
                statsMap.maximumWordLength = largestWord.length()
            }
            else {
                statsMap.maximumWordLength = 0
            }

            def medianWordLength = getMedianWordLength(wordCount)
            statsMap.medianWordLength = medianWordLength
        }

        return statsMap
    }

    
    def addToDataStore(List<String> wordsToAdd) {
        for (int i = 0 ; i < wordsToAdd.size() ; i++) {
            def word = wordsToAdd.get(i)
            def elemScore = redisService.zscore(ALL_WORDS_KEY, word) // this is to check whether the word exists yet

            if (elemScore == null) {
                // word does not exist, we need to add it
                def anagramGroupKey = generateKey(word)

                // we want to maintain atomicity and keep multiple redis keys in sync, hence withTrans
                redisService.withTransaction { Transaction transx ->
                    // adding duplicate data via zadd and sadd to redis for performance reasons
                    transx.zadd(ALL_WORDS_KEY, word.length(), word)
                    transx.sadd(anagramGroupKey, word)
                    transx.zincrby(FAMILY_COUNT_KEY, 1, anagramGroupKey)
                }
            }
            else {
                log.debug "skipped word: " + word
                continue // we want to skip this word and any operations surrounding it
            }
        }
        // calculating word avg for fast search on future reqs
        try {
            def wordAvg = calculateWordAvg()
            redisService.set(WORD_AVG_KEY, wordAvg.toString())
        } catch (Exception ex) {
            log.error "Exception occurred. ${ex?.message}", ex
            // going to delete WORD_AVG_KEY to ensure data correctness
            redisService.del(WORD_AVG_KEY)
        }
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
				throw new MalformedURLException("Please enter only valid alphabet chars.  No special chars please, you entered: ${c}")

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
        if (allWordsSize == 0) {
            return 0
        }
        def wordLengthSum = 0

        for (String word : allWords) {
            wordLengthSum += word.length()
        }

        return wordLengthSum.div(allWordsSize)
    }

    private getMedianWordLength(wordCount) {
        if ((wordCount % 2) == 0 ) {
            // word count is even
            long medianIndex = Math.floor(wordCount.div(2))
            Set medianWordSet = redisService.zrange(ALL_WORDS_KEY, medianIndex - 1, medianIndex)

            def sum = 0
            for (String word : medianWordSet) {
                sum+= word.length()
            }
            return sum.div(2)
        } 
        else {
            // word count is odd
            long medianIndex = Math.floor(wordCount.div(2))
            Set medianWordSet = redisService.zrange(ALL_WORDS_KEY, medianIndex, medianIndex)
            String medianWord = medianWordSet.iterator().next()
            return medianWord.length()
        }
    }
}
