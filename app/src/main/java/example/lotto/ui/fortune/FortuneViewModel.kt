// File Path: app/src/main/java/com/kimro/ai/lotto/ui/fortune/FortuneViewModel.kt
package com.kimro.ai.lotto.ui.fortune

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimro.ai.lotto.data.repository.LottoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

data class TarotCardInfo(
    val name: String,
    val keyword: String,
    val overallFortune: String,
    val positiveFlow: String,
    val cautionPoint: String,
    val todaysAdvice: String,
    val luckScore: Int,
    val wealthStars: Int,
    val loveStars: Int,
    val relationshipStars: Int,
    val luckyColor: String,
    val luckyNumber: Int,
    val luckyDirection: String
)

data class FortuneCheckIn(val cardName: String, val keyword: String, val luckScore: Int)

@HiltViewModel
class FortuneViewModel @Inject constructor(
    application: Application,
    private val repository: LottoRepository
) : AndroidViewModel(application) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun todayKey(): String = dateFormat.format(Date())

    private val prefs = application.getSharedPreferences("fortune_prefs", Context.MODE_PRIVATE)
    private val historyKey = "fortune_checkin_history"

    private val _tarotCardName = MutableStateFlow<String?>(null)
    val tarotCardName: StateFlow<String?> = _tarotCardName.asStateFlow()

    private val _tarotKeyword = MutableStateFlow<String?>(null)
    val tarotKeyword: StateFlow<String?> = _tarotKeyword.asStateFlow()

    private val _tarotMeaningPositive = MutableStateFlow<String?>(null)
    val tarotMeaningPositive: StateFlow<String?> = _tarotMeaningPositive.asStateFlow()

    private val _tarotMeaningNegative = MutableStateFlow<String?>(null)
    val tarotMeaningNegative: StateFlow<String?> = _tarotMeaningNegative.asStateFlow()

    private val _selectedCardInfo = MutableStateFlow<TarotCardInfo?>(null)
    val selectedCardInfo: StateFlow<TarotCardInfo?> = _selectedCardInfo.asStateFlow()

    private val _tarotCardOptions = MutableStateFlow<List<TarotCardInfo>>(emptyList())
    val tarotCardOptions: StateFlow<List<TarotCardInfo>> = _tarotCardOptions.asStateFlow()

    private val _selectedCardIndex = MutableStateFlow<Int?>(null)
    val selectedCardIndex: StateFlow<Int?> = _selectedCardIndex.asStateFlow()

    private val _generatedTarotSets = MutableStateFlow<List<List<Int>>>(emptyList())
    val generatedTarotSets: StateFlow<List<List<Int>>> = _generatedTarotSets.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val _hasDrawnToday = MutableStateFlow(false)
    val hasDrawnToday: StateFlow<Boolean> = _hasDrawnToday.asStateFlow()

    private val _checkInHistory = MutableStateFlow<Map<String, FortuneCheckIn>>(emptyMap())
    val checkInHistory: StateFlow<Map<String, FortuneCheckIn>> = _checkInHistory.asStateFlow()

    private var pendingSetCount: Int = 5

    private val tarotDeck = listOf(
        TarotCardInfo(
            name = "0. 바보 (The Fool)",
            keyword = "새로운 시작 · 자유로운 도전",
            overallFortune = "계획에 없던 새로운 시도나 즉흥적인 선택이 신선한 변화를 불러오는 하루입니다.",
            positiveFlow = "틀에 박힌 사고방식에서 벗어나 가벼운 마음으로 도전하면 뜻밖의 활력을 얻을 수 있습니다.",
            cautionPoint = "준비되지 않은 상태에서 무모하게 움직이거나 충동적인 지출을 하면 낭패를 볼 수 있습니다.",
            todaysAdvice = "가벼운 호기심은 좋으나 실속 없는 요행은 멀리하고 주변을 살피세요.",
            luckScore = 65, wealthStars = 2, loveStars = 3, relationshipStars = 3,
            luckyColor = "보라색", luckyNumber = 0, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "1. 마법사 (The Magician)",
            keyword = "재능 발휘 · 창조적 기회",
            overallFortune = "당신이 가진 경험과 재능을 활용하여 주변 상황을 유리하게 주도할 수 있는 국면입니다.",
            positiveFlow = "아이디어가 반짝이는 날이므로 중요한 계획이나 결정을 추진하기에 아주 적합합니다.",
            cautionPoint = "말만 앞서거나 지나친 요령을 피우다 오히려 신뢰를 잃을 수 있으니 진중함을 유지하세요.",
            todaysAdvice = "가진 능력을 믿고 자신 있게 밀어붙이되, 겸손한 태도를 잃지 마세요.",
            luckScore = 85, wealthStars = 4, loveStars = 3, relationshipStars = 4,
            luckyColor = "빨간색", luckyNumber = 1, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "2. 여사제 (The High Priestess)",
            keyword = "철저한 관망 · 냉철한 직관",
            overallFortune = "감정에 치우치지 않고 냉정하게 상황을 분석하는 통찰력이 빛을 발하는 시기입니다.",
            positiveFlow = "불확실한 정보나 남의 말에 휘둘리지 않고 본인의 직관을 믿으면 현명한 답을 찾습니다.",
            cautionPoint = "지나치게 마음의 문을 닫아걸거나 의심이 지나쳐 소중한 조언마저 차단할 수 있습니다.",
            todaysAdvice = "성급하게 결론을 내리지 말고 한 발 물러서서 상황을 차분히 관망하세요.",
            luckScore = 70, wealthStars = 3, loveStars = 2, relationshipStars = 3,
            luckyColor = "남색", luckyNumber = 2, luckyDirection = "북쪽"
        ),
        TarotCardInfo(
            name = "3. 여황제 (The Empress)",
            keyword = "풍요로운 결실 · 안정적 흐름",
            overallFortune = "그동안 공들였던 일에서 물질적·심리적인 안정과 만족스러운 성과를 거두는 운세입니다.",
            positiveFlow = "금전적으로 여유가 생기거나 뜻밖의 소소한 이득이 들어오는 편안한 하루입니다.",
            cautionPoint = "현재의 안락함에 취해 나태해지거나 불필요한 사치와 과소비로 지출이 늘 수 있습니다.",
            todaysAdvice = "주어진 풍요에 감사하되, 자금 관리에 대한 긴장감은 끝까지 유지하세요.",
            luckScore = 88, wealthStars = 5, loveStars = 4, relationshipStars = 4,
            luckyColor = "초록색", luckyNumber = 3, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "4. 황제 (The Emperor)",
            keyword = "확고한 통제 · 현실적 성취",
            overallFortune = "조직이나 자금 흐름을 주도적으로 통제하며 실질적인 성과와 권위를 쟁취하는 형국입니다.",
            positiveFlow = "철저한 원칙과 계획대로 밀어붙이면 원하던 목표를 확고하게 달성할 수 있습니다.",
            cautionPoint = "권위적인 태도나 지나친 고집으로 인해 주변 사람들과 불필요한 마찰이 빚어질 수 있습니다.",
            todaysAdvice = "확고한 리더십을 발휘하되 타인의 의견도 유연하게 수용하는 지혜가 필요합니다.",
            luckScore = 84, wealthStars = 4, loveStars = 3, relationshipStars = 3,
            luckyColor = "주황색", luckyNumber = 4, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "5. 교황 (The Hierophant)",
            keyword = "멘토의 조언 · 원칙 준수",
            overallFortune = "독단적인 행동보다는 기존의 관례나 신뢰할 수 있는 멘토의 조언을 따르는 것이 유리합니다.",
            positiveFlow = "정석대로 절차를 밟아가면 안정적이고 신뢰성 있는 결과를 확보할 수 있습니다.",
            cautionPoint = "지나친 형식과 틀에 얽매여 유연한 대처를 하지 못하면 오히려 손해를 볼 수 있습니다.",
            todaysAdvice = "주변의 검증된 가이드라인을 참고하고 도덕적인 태도를 유지하세요.",
            luckScore = 72, wealthStars = 3, loveStars = 3, relationshipStars = 4,
            luckyColor = "회갈색", luckyNumber = 5, luckyDirection = "북쪽"
        ),
        TarotCardInfo(
            name = "6. 연인 (The Lovers)",
            keyword = "조화와 소통 · 매력 발산",
            overallFortune = "주변 사람들과의 교감이 깊어지고 좋은 인연이나 협력자를 만날 수 있는 운세입니다.",
            positiveFlow = "마음이 잘 맞는 사람과 뜻깊은 프로젝트나 대화를 나누며 시너지를 낼 수 있습니다.",
            cautionPoint = "중요한 선택의 기로에서 감정에 치우쳐 우유부단하게 행동하면 결정을 그릇칠 수 있습니다.",
            todaysAdvice = "마음의 소리에 귀 기울이되 이성적인 판단의 균형을 잃지 마세요.",
            luckScore = 86, wealthStars = 3, loveStars = 5, relationshipStars = 5,
            luckyColor = "분홍색", luckyNumber = 6, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "7. 전차 (The Chariot)",
            keyword = "정면 돌파 · 속도감 있는 진행",
            overallFortune = "장기화되던 정체 구간을 시원하게 벗어나며 과감한 결단으로 승기를 잡는 기운입니다.",
            positiveFlow = "의욕이 최고조에 달하므로 밀어붙이는 일마다 가속도가 붙고 성취감이 큽니다.",
            cautionPoint = "속도에만 집착한 나머지 주변의 위험 요소를 놓치거나 무리한 충돌을 유발할 수 있습니다.",
            todaysAdvice = "열정을 다해 앞으로 나아가되, 브레이크를 잡아야 할 때를 잊지 마세요.",
            luckScore = 91, wealthStars = 4, loveStars = 3, relationshipStars = 3,
            luckyColor = "은색", luckyNumber = 7, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "8. 힘 (Strength)",
            keyword = "부드러운 통제 · 끈기와 인내",
            overallFortune = "강압적인 방식보다 부드러움과 끈기로 난관을 슬기롭게 극복해내는 날입니다.",
            positiveFlow = "끈질긴 인내심을 발휘하면 까다롭던 상대나 상황을 내 편으로 만들 수 있습니다.",
            cautionPoint = "내면의 스트레스나 분노를 꾹 참기만 하다가 한 번에 폭발할 수 있으니 주의하세요.",
            todaysAdvice = "조급해하지 말고 포용력 있는 태도로 장기적인 관점에서 접근하세요.",
            luckScore = 76, wealthStars = 3, loveStars = 4, relationshipStars = 4,
            luckyColor = "황금색", luckyNumber = 8, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "9. 은둔자 (The Hermit)",
            keyword = "내면 성찰 · 깊은 연구",
            overallFortune = "외부 활동을 줄이고 혼자만의 시간을 가지며 내면을 깊이 돌아보는 시기입니다.",
            positiveFlow = "철저한 분석과 사색을 통해 그동안 풀리지 않던 문제의 해답을 문득 깨닫게 됩니다.",
            cautionPoint = "지나치게 세상과 담을 쌓거나 소통을 거부하면 주변과의 유대감이 약화될 수 있습니다.",
            todaysAdvice = "조용히 내실을 다지고 지혜를 충전하는 소중한 기회로 활용하세요.",
            luckScore = 68, wealthStars = 2, loveStars = 2, relationshipStars = 2,
            luckyColor = "회색", luckyNumber = 9, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "10. 운명의 수레바퀴 (Wheel of Fortune)",
            keyword = "흐름의 반전 · 뜻밖의 행운",
            overallFortune = "정체되어 있던 상황의 흐름이 유리한 방향으로 급격하게 전환되는 강력한 대길운입니다.",
            positiveFlow = "예상치 못한 경로를 통해 재물이나 귀한 기회가 찾아올 수 있는 드라마틱한 시점입니다.",
            cautionPoint = "상황 변화가 빠른 만큼 방심하는 사이에 유리했던 국면이 순식간에 뒤바뀔 수도 있습니다.",
            todaysAdvice = "찾아온 행운의 흐름을 기쁘게 받아들이되, 겸손한 자세를 유지하세요.",
            luckScore = 96, wealthStars = 5, loveStars = 4, relationshipStars = 4,
            luckyColor = "금색", luckyNumber = 10, luckyDirection = "중앙"
        ),
        TarotCardInfo(
            name = "11. 정의 (Justice)",
            keyword = "공정함 · 객관적 판단",
            overallFortune = "사사로운 감정을 배제하고 철저하게 공정하고 객관적으로 판단해야 하는 하루입니다.",
            positiveFlow = "원칙에 따른 공정한 계약이나 서류 처리, 혹은 합리적인 문제 해결이 이루어집니다.",
            cautionPoint = "지나치게 따지거나 냉정한 잣대를 들이대어 주변 사람들에게 상처를 줄 수 있습니다.",
            todaysAdvice = "균형 잡힌 시각을 유지하고 매사에 투명하고 정직하게 처신하세요.",
            luckScore = 75, wealthStars = 3, loveStars = 3, relationshipStars = 3,
            luckyColor = "청색", luckyNumber = 11, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "12. 매달린 사람 (The Hanged Man)",
            keyword = "정체와 답답함 · 관점의 전환",
            overallFortune = "애써 노력해도 보상이 곧바로 뒤따르지 않고 진퇴양난에 빠져 답답함을 느낄 수 있습니다.",
            positiveFlow = "현재의 정체기를 숨 고르기 기간으로 삼는다면 오히려 장기적인 자양분이 됩니다.",
            cautionPoint = "억지로 상황을 뒤집으려 하면 피로감만 가중되니 무리한 행동은 금물입니다.",
            todaysAdvice = "생각의 틀을 바꾸어 바라보면 전혀 다른 해결책이 보일 수 있습니다.",
            luckScore = 45, wealthStars = 2, loveStars = 2, relationshipStars = 2,
            luckyColor = "청록색", luckyNumber = 12, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "13. 죽음 (Death)",
            keyword = "과감한 단절 · 새로운 탈바꿈",
            overallFortune = "불필요하게 끌어오던 부실한 관계나 소모적인 일을 과감하게 끊어내야 하는 시점입니다.",
            positiveFlow = "묵은 것을 정리함으로써 새로운 기회가 들어올 수 있는 확실한 정화 작용이 일어납니다.",
            cautionPoint = "예상치 못한 손실이나 오랫동안 공들였던 계획의 마무리를 마주하며 충격을 받을 수 있습니다.",
            todaysAdvice = "미련을 가감 없이 버리고 다가올 새로운 변화를 기꺼이 받아들이세요.",
            luckScore = 35, wealthStars = 1, loveStars = 1, relationshipStars = 1,
            luckyColor = "검은색", luckyNumber = 13, luckyDirection = "서쪽"
        ),
        TarotCardInfo(
            name = "14. 절제 (Temperance)",
            keyword = "균형과 조화 · 소통과 치유",
            overallFortune = "서로 다른 의견이나 상황 속에서 적절한 타협점을 찾아 평온을 회복하는 날입니다.",
            positiveFlow = "무리하지 않고 페이스를 조절하며 진행하는 일마다 순조로운 조화를 이룹니다.",
            cautionPoint = "지나친 양보나 중간 입장에서 이도 저도 아닌 태도를 취하면 혼란이 가중됩니다.",
            todaysAdvice = "급하게 서두르지 말고 중용의 미덕을 지키며 평정심을 유지하세요.",
            luckScore = 80, wealthStars = 3, loveStars = 4, relationshipStars = 4,
            luckyColor = "하늘색", luckyNumber = 14, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "15. 악마 (The Devil)",
            keyword = "유혹과 집착 · 요행 경계",
            overallFortune = "단기간에 큰돈을 벌려는 투기 심리나 달콤한 유혹에 흔들리기 쉬운 위험한 날입니다.",
            positiveFlow = "자신의 내면에 숨겨진 욕망과 집착이 무엇인지 솔직하게 자각하는 계기가 됩니다.",
            cautionPoint = "불확실한 투자 제안이나 감언이설에 속아 자칫 큰 금전적 손실을 입을 수 있습니다.",
            todaysAdvice = "요행을 바라지 말고 원칙을 지키며 부정적인 유혹을 철저히 차단하세요.",
            luckScore = 30, wealthStars = 1, loveStars = 2, relationshipStars = 2,
            luckyColor = "와인색", luckyNumber = 15, luckyDirection = "남쪽"
        ),
        TarotCardInfo(
            name = "16. 탑 (The Tower)",
            keyword = "갑작스러운 타격 · 재정비",
            overallFortune = "예상하지 못한 변화나 충격적인 소식으로 기존의 계획이 흔들릴 수 있습니다.",
            positiveFlow = "오랫동안 썩어 있던 문제를 과감하게 도려내고 기초부터 다시 다지는 기회가 됩니다.",
            cautionPoint = "충동적인 결정이나 무리한 금전 지출, 감정적인 충돌은 절대적으로 피해야 합니다.",
            todaysAdvice = "변화를 억지로 막기보다 상황을 받아들이고 리스크 관리에 총력을 기울이세요.",
            luckScore = 25, wealthStars = 1, loveStars = 1, relationshipStars = 1,
            luckyColor = "차콜색", luckyNumber = 16, luckyDirection = "북쪽"
        ),
        TarotCardInfo(
            name = "17. 별 (The Star)",
            keyword = "희망과 영감 · 밝은 비전",
            overallFortune = "어두운 터널을 지나 마음속에 희망의 불빛과 새로운 영감이 샘솟는 치유의 날입니다.",
            positiveFlow = "앞날에 대한 비전이 뚜렷해지고 사람들에게 인기를 얻거나 칭찬을 받을 수 있습니다.",
            cautionPoint = "지나치게 이상적인 목표만 좇다가 현실적인 실천을 소홀히 할 수 있습니다.",
            todaysAdvice = "희망을 품고 차근차근 계획을 실행에 옮기면 반드시 빛을 보게 됩니다.",
            luckScore = 89, wealthStars = 4, loveStars = 5, relationshipStars = 4,
            luckyColor = "남청색", luckyNumber = 17, luckyDirection = "북동쪽"
        ),
        TarotCardInfo(
            name = "18. 달 (The Moon)",
            keyword = "불안감 증폭 · 정보 혼선",
            overallFortune = "실체 없는 불안감과 막연한 의심이 커져 중요한 판단을 그릇치기 쉬운 형국입니다.",
            positiveFlow = "직관이나 감수성이 예민해져 남들이 보지 못하는 이면의 미묘한 분위기를 감지합니다.",
            cautionPoint = "명확하지 않은 소문이나 루머에 현혹되어 금전적 손해나 오해를 살 수 있습니다.",
            todaysAdvice = "마음을 편히 먹고 팩트 위주로 상황을 확인하며 불안감을 다스리세요.",
            luckScore = 50, wealthStars = 2, loveStars = 2, relationshipStars = 2,
            luckyColor = "짙은회색", luckyNumber = 18, luckyDirection = "북서쪽"
        ),
        TarotCardInfo(
            name = "19. 태양 (The Sun)",
            keyword = "최고의 성취 · 밝은 미래",
            overallFortune = "모든 어둠과 고민이 걷히고 명확한 해답과 눈부신 성과가 기다리고 있는 최고의 길운입니다.",
            positiveFlow = "막혀 있던 자금 흐름이나 인간관계가 시원하게 풀리며 자신감이 충만해집니다.",
            cautionPoint = "지나친 낙관주의에 빠져 사소한 리스크나 디테일을 놓치고 지나갈 수 있습니다.",
            todaysAdvice = "밝은 에너지를 만끽하되 마지막 순간까지 주변을 살피는 신중함을 유지하세요.",
            luckScore = 98, wealthStars = 5, loveStars = 5, relationshipStars = 5,
            luckyColor = "노란색", luckyNumber = 19, luckyDirection = "동쪽"
        ),
        TarotCardInfo(
            name = "20. 심판 (Judgement)",
            keyword = "결과 발표 · 극적인 부활",
            overallFortune = "그동안 노력해온 일에 대한 공식적인 평가나 보상이 극적으로 주어지는 날입니다.",
            positiveFlow = "포기했던 일이나 기회가 다시 살아나며 명확한 해결의 실마리를 잡게 됩니다.",
            cautionPoint = "과거의 실수나 미련에 집착하여 앞으로 나아갈 기회를 놓치지 않도록 주의하세요.",
            todaysAdvice = "과거를 깨끗이 정리하고 다가오는 중요한 결정에 집중하세요.",
            luckScore = 92, wealthStars = 4, loveStars = 4, relationshipStars = 4,
            luckyColor = "주황빛붉은색", luckyNumber = 20, luckyDirection = "중앙"
        ),
        TarotCardInfo(
            name = "21. 세계 (The World)",
            keyword = "완벽한 완성 · 목표 달성",
            overallFortune = "오랫동안 공들여온 거대한 프로젝트나 목표가 마침내 완벽하게 마무리되는 대단원입니다.",
            positiveFlow = "마음의 평화와 함께 주변의 찬사를 받으며 다음 단계로 행복하게 도약합니다.",
            cautionPoint = "모든 것을 이뤘다는 안도감에 다음 목표를 향한 동력을 잃어버릴 수 있습니다.",
            todaysAdvice = "성취를 온전히 자축하고 다음 단계를 위한 아름다운 마무리를 지으세요.",
            luckScore = 99, wealthStars = 5, loveStars = 5, relationshipStars = 5,
            luckyColor = "남색", luckyNumber = 21, luckyDirection = "전방위"
        )
    )

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val raw = prefs.getString(historyKey, null) ?: return
        try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, FortuneCheckIn>()
            json.keys().forEach { dateKey ->
                val obj = json.getJSONObject(dateKey)
                map[dateKey] = FortuneCheckIn(
                    cardName = obj.getString("card"),
                    keyword = obj.optString("keyword", ""),
                    luckScore = obj.getInt("score")
                )
            }
            _checkInHistory.value = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistory(map: Map<String, FortuneCheckIn>) {
        val json = JSONObject()
        map.forEach { (date, checkIn) ->
            val obj = JSONObject()
            obj.put("card", checkIn.cardName)
            obj.put("keyword", checkIn.keyword)
            obj.put("score", checkIn.luckScore)
            json.put(date, obj)
        }
        prefs.edit().putString(historyKey, json.toString()).apply()
    }

    fun drawTarotCards(setCount: Int) {
        pendingSetCount = setCount
        _tarotCardOptions.value = tarotDeck.shuffled().take(3)
        _selectedCardIndex.value = null
        _selectedCardInfo.value = null
        _tarotCardName.value = null
        _tarotKeyword.value = null
        _tarotMeaningPositive.value = null
        _tarotMeaningNegative.value = null
        _generatedTarotSets.value = emptyList()
    }

    fun selectTarotCard(index: Int) {
        if (_tarotCardOptions.value.isEmpty()) {
            _tarotCardOptions.value = tarotDeck.shuffled().take(3)
        }

        val options = _tarotCardOptions.value
        if (index !in options.indices) return

        _selectedCardIndex.value = index

        val selectedCard = options[index]
        _selectedCardInfo.value = selectedCard
        _tarotCardName.value = selectedCard.name
        _tarotKeyword.value = selectedCard.keyword
        _tarotMeaningPositive.value = selectedCard.positiveFlow
        _tarotMeaningNegative.value = selectedCard.cautionPoint

        regenerateLuckyNumbers()

        val newHistory = _checkInHistory.value.toMutableMap()
        newHistory[todayKey()] = FortuneCheckIn(selectedCard.name, selectedCard.keyword, selectedCard.luckScore)
        _checkInHistory.value = newHistory
        saveHistory(newHistory)
    }

    fun updateSetCount(setCount: Int) {
        pendingSetCount = setCount
        if (_selectedCardInfo.value != null) {
            regenerateLuckyNumbers()
        }
    }

    private fun regenerateLuckyNumbers() {
        val sets = mutableListOf<List<Int>>()
        val seed = System.currentTimeMillis()
        val cardLuckyNumber = _selectedCardInfo.value?.luckyNumber ?: 0

        // 5개 및 10개 조합 모두 1번(인덱스 0), 3번(인덱스 2) 조합에만 행운번호 부여
        val targetIndices = listOf(0, 2)

        for (i in 0 until pendingSetCount) {
            val setRandom = Random(seed + i * 137)
            val resultSet = mutableSetOf<Int>()

            if (i in targetIndices && cardLuckyNumber in 1..45) {
                resultSet.add(cardLuckyNumber)
            }

            while (resultSet.size < 6) {
                val candidate = setRandom.nextInt(1, 46)
                if (!resultSet.contains(candidate)) {
                    var hasConsecutive = false
                    for (existing in resultSet) {
                        if (kotlin.math.abs(existing - candidate) == 1) {
                            hasConsecutive = true
                            break
                        }
                    }
                    if (!hasConsecutive || resultSet.size >= 4) {
                        resultSet.add(candidate)
                    }
                }
            }
            sets.add(resultSet.sorted())
        }
        _generatedTarotSets.value = sets
    }

    fun saveAllTarotNumbers() {
        val currentSets = _generatedTarotSets.value
        if (currentSets.isNotEmpty()) {
            viewModelScope.launch {
                currentSets.forEach { numbers ->
                    repository.insertLotto(numbers, "TAROT")
                }
                _saveMessage.value = "✓ 내 번호에 저장되었습니다"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
