const pptxgen = require("pptxgenjs");

const p = new pptxgen();
p.layout = "LAYOUT_WIDE"; // 13.33 x 7.5
p.author = "민규";
p.title = "말씀벗 피치덱";

// ── 팔레트: 딥 네이비(신뢰) + 웜 골드(성스러움) ──
const INK = "1C2B46";   // 딥 네이비 — 표지/마무리 배경, 제목
const INK2 = "27395C";  // 밝은 네이비 — 다크 카드
const GOLD = "BE8C36";  // 웜 골드 — accent
const GOLDLT = "E4C988"; // 옅은 골드
const WHITE = "FFFFFF";
const SLATE = "5B6573"; // 보조 텍스트
const MIST = "F2F4F8";  // 카드 틴트
const LINE = "DDE2EA";
const FONT = "Malgun Gothic";

const W = 13.33, H = 7.5;
const cardShadow = () => ({ type: "outer", color: "9AA3B0", blur: 9, offset: 3, angle: 90, opacity: 0.22 });

function header(slide, title, sub) {
  slide.addText(title, { x: 0.7, y: 0.5, w: 12, h: 0.7, fontFace: FONT, fontSize: 30, bold: true, color: INK, margin: 0 });
  if (sub) slide.addText(sub, { x: 0.7, y: 1.18, w: 12, h: 0.4, fontFace: FONT, fontSize: 14, color: SLATE, margin: 0 });
}
function footer(slide, n) {
  slide.addText("말씀벗", { x: 0.7, y: 7.04, w: 3, h: 0.3, fontFace: FONT, fontSize: 9, color: SLATE, margin: 0 });
  slide.addText(String(n), { x: 12.4, y: 7.04, w: 0.4, h: 0.3, fontFace: FONT, fontSize: 9, color: SLATE, align: "right", margin: 0 });
}
function numCircle(slide, x, y, label) {
  slide.addShape(p.shapes.OVAL, { x, y, w: 0.5, h: 0.5, fill: { color: GOLD } });
  slide.addText(label, { x, y, w: 0.5, h: 0.5, fontFace: FONT, fontSize: 16, bold: true, color: WHITE, align: "center", valign: "middle", margin: 0 });
}

// ════════ 1. 표지 ════════
{
  const s = p.addSlide();
  s.background = { color: INK };
  s.addText("AI 신앙 동반자", { x: 0.95, y: 1.85, w: 8, h: 0.4, fontFace: FONT, fontSize: 14, color: GOLDLT, charSpacing: 4, margin: 0 });
  s.addText("말씀벗", { x: 0.9, y: 2.35, w: 9, h: 1.5, fontFace: FONT, fontSize: 66, bold: true, color: GOLD, margin: 0 });
  s.addText("현생에 치여 하나님을 잊고 사는 크리스찬에게,\n필요한 순간 성경 말씀으로 답해주는 AI 신앙 동반자", { x: 0.95, y: 3.95, w: 9.5, h: 1.1, fontFace: FONT, fontSize: 19, color: WHITE, lineSpacingMultiple: 1.25, margin: 0 });
  s.addText("제품기획서 · 2026", { x: 0.95, y: 6.55, w: 6, h: 0.4, fontFace: FONT, fontSize: 12, color: "9FB0C9", margin: 0 });
  // 따옴표 모티프
  s.addText("”", { x: 9.9, y: 2.7, w: 3, h: 3, fontFace: "Georgia", fontSize: 300, color: INK2, align: "center", valign: "middle", margin: 0 });
}

// ════════ 2. 문제 ════════
{
  const s = p.addSlide();
  header(s, "현생에 치여 말씀과 단절된 크리스찬", "신앙은 있지만 일상에서 하나님을 만날 접점이 없습니다");
  const probs = [
    ["말씀과 단절된 일상", "신앙은 있지만, 현생에 치여 말씀을 잊고 삽니다."],
    ["안 읽히는 일방적 푸시", "한 줄 말씀 알림은 오픈율이 낮고 맥락이 없습니다."],
    ["물어볼 곳 없는 질문", "목사님껜 부담, 검색하면 이단·논쟁 글이 섞입니다."],
    ["쌓이는 죄책감", "교회를 못 가는 동안 정죄감만 커집니다."],
  ];
  const cw = 5.75, ch = 2.1, gx = 0.7, gy = 1.9, gapX = 0.45, gapY = 0.4;
  probs.forEach((pr, i) => {
    const x = gx + (i % 2) * (cw + gapX);
    const y = gy + Math.floor(i / 2) * (ch + gapY);
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w: cw, h: ch, fill: { color: MIST }, rectRadius: 0.1, shadow: cardShadow() });
    numCircle(s, x + 0.4, y + 0.42, String(i + 1));
    s.addText(pr[0], { x: x + 1.1, y: y + 0.4, w: cw - 1.4, h: 0.55, fontFace: FONT, fontSize: 19, bold: true, color: INK, valign: "middle", margin: 0 });
    s.addText(pr[1], { x: x + 0.45, y: y + 1.1, w: cw - 0.9, h: 0.8, fontFace: FONT, fontSize: 14, color: SLATE, lineSpacingMultiple: 1.2, margin: 0 });
  });
  footer(s, 2);
}

// ════════ 3. 솔루션 ════════
{
  const s = p.addSlide();
  header(s, "대화로 만나는, 성경 기반 신앙 동반자", "필요한 순간 내 상황을 말하면, 그에 맞는 말씀으로 답합니다");
  const sols = [
    ["대화형", "푸시가 아니라, 내 상황을 말하면\n그에 맞는 말씀으로 응답합니다."],
    ["성경 기반", "모든 답변은 성경 본문을 근거로.\nAI의 사견과 환각을 막습니다."],
    ["동반자", "목회자·교회를 대체하지 않고\n연결하는 '다리'가 됩니다."],
  ];
  const cw = 3.75, ch = 3.4, gy = 2.1, gapX = 0.5;
  const totalW = cw * 3 + gapX * 2, gx = (W - totalW) / 2;
  sols.forEach((sl, i) => {
    const x = gx + i * (cw + gapX);
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y: gy, w: cw, h: ch, fill: { color: WHITE }, line: { color: LINE, width: 1 }, rectRadius: 0.1, shadow: cardShadow() });
    s.addShape(p.shapes.OVAL, { x: x + cw / 2 - 0.4, y: gy + 0.5, w: 0.8, h: 0.8, fill: { color: INK } });
    s.addText(String(i + 1), { x: x + cw / 2 - 0.4, y: gy + 0.5, w: 0.8, h: 0.8, fontFace: FONT, fontSize: 26, bold: true, color: GOLDLT, align: "center", valign: "middle", margin: 0 });
    s.addText(sl[0], { x: x + 0.3, y: gy + 1.55, w: cw - 0.6, h: 0.6, fontFace: FONT, fontSize: 22, bold: true, color: GOLD, align: "center", margin: 0 });
    s.addText(sl[1], { x: x + 0.35, y: gy + 2.2, w: cw - 0.7, h: 1.0, fontFace: FONT, fontSize: 14.5, color: SLATE, align: "center", lineSpacingMultiple: 1.25, margin: 0 });
  });
  footer(s, 3);
}

// ════════ 4. 제품 — 6갈래 자동 분기 ════════
{
  const s = p.addSlide();
  header(s, "무슨 말을 하든, 알아서 갈래를 탑니다", "사용자는 모드를 고를 필요가 없습니다 — 6갈래 자동 분류");
  const rows = [
    [{ text: "이렇게 말하면", options: { fill: { color: INK }, color: WHITE, bold: true, fontSize: 15, align: "center" } },
     { text: "말씀벗이 하는 일", options: { fill: { color: INK }, color: WHITE, bold: true, fontSize: 15, align: "center" } }],
    ["“요즘 너무 힘들어”", "공감 → 상황에 맞는 성경 구절 → 짧은 적용"],
    ["“면접 전인데 기도해줘”", "그 상황을 담은 맞춤 기도문 작성"],
    ["“방언이 뭐야?”", "성경 근거로 답하되 교단은 중립"],
    ["“오늘 날씨 좋다”", "친구처럼 받아줌 (말씀 강요 안 함)"],
    ["“죽고 싶어”", "신앙 답변 멈추고 109 즉시 연결"],
    ["“코드 짜줘”", "정중히 거절 + 할 수 있는 것 안내"],
  ];
  const styled = rows.map((r, ri) => r.map((c, ci) => {
    if (ri === 0) return c;
    const base = { fontFace: FONT, fontSize: 14.5, color: ri === 5 ? "A32D2D" : INK, valign: "middle",
      fill: { color: ri % 2 === 1 ? MIST : WHITE }, margin: [4, 8, 4, 8] };
    if (ci === 0) return { text: c, options: { ...base, bold: true } };
    return { text: c, options: { ...base, color: ri === 5 ? "A32D2D" : SLATE } };
  }));
  s.addTable(styled, { x: 0.7, y: 1.85, w: 11.93, colW: [4.3, 7.63], rowH: 0.62, border: { type: "solid", pt: 1, color: LINE }, fontFace: FONT });
  s.addText("위기(“죽고 싶어”)는 다른 모든 분기에 우선합니다.", { x: 0.7, y: 6.5, w: 12, h: 0.4, fontFace: FONT, fontSize: 13, italic: true, color: GOLD, margin: 0 });
  footer(s, 4);
}

// ════════ 5. 차별점 — ChatGPT엔 없는 두 가지 ════════
{
  const s = p.addSlide();
  header(s, "ChatGPT엔 없는, 두 가지 안전장치", "범용 AI와 범용성으로 경쟁하지 않습니다 — 신뢰로 경쟁합니다");
  const cards = [
    ["성경 구절을 지어내지 않습니다", "ChatGPT는 그럴듯한 구절을 만들어냅니다. 말씀벗은 DB에 없는 구절을 거부하고, 모든 인용을 검증된 원문으로만 제시합니다."],
    ["위기는 그 무엇보다 우선합니다", "자살·자해 신호를 감지하면 신앙 답변을 멈추고 전문기관(109)으로 즉시 연결합니다. 안전을 신학·기능보다 위에 둡니다."],
  ];
  const cw = 5.75, ch = 3.0, gy = 2.0, gapX = 0.45, gx = 0.7;
  cards.forEach((c, i) => {
    const x = gx + i * (cw + gapX);
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y: gy, w: cw, h: ch, fill: { color: i === 1 ? INK : MIST }, rectRadius: 0.1, shadow: cardShadow() });
    s.addText(i === 0 ? "①" : "②", { x: x + 0.4, y: gy + 0.35, w: 1, h: 0.6, fontFace: FONT, fontSize: 28, bold: true, color: GOLD, margin: 0 });
    s.addText(c[0], { x: x + 0.45, y: gy + 1.0, w: cw - 0.9, h: 0.7, fontFace: FONT, fontSize: 19, bold: true, color: i === 1 ? WHITE : INK, valign: "middle", margin: 0 });
    s.addText(c[1], { x: x + 0.45, y: gy + 1.7, w: cw - 0.9, h: 1.1, fontFace: FONT, fontSize: 14, color: i === 1 ? "C9D4E5" : SLATE, lineSpacingMultiple: 1.28, margin: 0 });
  });
  s.addText("+  한국 개신교 신학 가드레일 · 교단 중립 · 정죄 없는 태도", { x: 0.7, y: 5.4, w: 11.93, h: 0.6, fontFace: FONT, fontSize: 15, bold: true, color: GOLD, align: "center", valign: "middle", margin: 0 });
  footer(s, 5);
}

// ════════ 6. 시장 ════════
{
  const s = p.addSlide();
  header(s, "검증된 수요, 확장 가능한 시장", "국내 핵심 타겟이 분명하고, 글로벌은 성공 사례가 증명합니다");
  const stats = [
    ["800~900만", "국내 개신교 인구", "핵심 타겟인 2040 '가나안 성도' 비중 증가"],
    ["5억+", "YouVersion 글로벌 다운로드", "기독교 앱 시장의 규모를 증명"],
    ["월 구독", "Hallow(미국·가톨릭)", "유료 구독 모델 성공을 입증"],
  ];
  const cw = 3.75, ch = 3.5, gy = 2.1, gapX = 0.5;
  const totalW = cw * 3 + gapX * 2, gx = (W - totalW) / 2;
  stats.forEach((st, i) => {
    const x = gx + i * (cw + gapX);
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y: gy, w: cw, h: ch, fill: { color: MIST }, rectRadius: 0.1, shadow: cardShadow() });
    s.addText(st[0], { x: x + 0.2, y: gy + 0.5, w: cw - 0.4, h: 1.1, fontFace: FONT, fontSize: 40, bold: true, color: GOLD, align: "center", valign: "middle", margin: 0 });
    s.addText(st[1], { x: x + 0.3, y: gy + 1.7, w: cw - 0.6, h: 0.5, fontFace: FONT, fontSize: 16, bold: true, color: INK, align: "center", margin: 0 });
    s.addText(st[2], { x: x + 0.35, y: gy + 2.25, w: cw - 0.7, h: 1.0, fontFace: FONT, fontSize: 13, color: SLATE, align: "center", lineSpacingMultiple: 1.25, margin: 0 });
  });
  footer(s, 6);
}

// ════════ 7. 수익 모델 ════════
{
  const s = p.addSlide();
  header(s, "단계적 수익화 — 검증 후 구독, 그리고 B2B", "무료로 검증하고, 구독으로 확장하고, 교회 라이선스로 키웁니다");
  const tiers = [
    ["Phase 1", "무료", "검증 단계 · 일 5회 제한\n경량 모델로 비용 통제"],
    ["Phase 2", "월 3,900원", "무제한 대화 + 기도제목 관리\n+ 감사일기 (Freemium 구독)"],
    ["Phase 3", "교회 월 5~20만", "B2B 라이선스 · 공식 챗봇\n새신자 케어 자동화"],
  ];
  const cw = 3.75, ch = 3.0, gy = 1.95, gapX = 0.5;
  const totalW = cw * 3 + gapX * 2, gx = (W - totalW) / 2;
  tiers.forEach((t, i) => {
    const x = gx + i * (cw + gapX);
    const dark = i === 1;
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y: gy, w: cw, h: ch, fill: { color: dark ? INK : MIST }, rectRadius: 0.1, shadow: cardShadow() });
    s.addText(t[0], { x: x + 0.35, y: gy + 0.35, w: cw - 0.7, h: 0.4, fontFace: FONT, fontSize: 14, bold: true, color: dark ? GOLDLT : SLATE, margin: 0 });
    s.addText(t[1], { x: x + 0.35, y: gy + 0.85, w: cw - 0.7, h: 0.8, fontFace: FONT, fontSize: 26, bold: true, color: GOLD, margin: 0 });
    s.addText(t[2], { x: x + 0.35, y: gy + 1.75, w: cw - 0.7, h: 1.0, fontFace: FONT, fontSize: 13.5, color: dark ? "C9D4E5" : SLATE, lineSpacingMultiple: 1.28, margin: 0 });
  });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: gx, y: gy + ch + 0.35, w: totalW, h: 0.85, fill: { color: "F6EFDF" }, rectRadius: 0.08 });
  s.addText([
    { text: "사용자당 API 비용 월 300~800원", options: { color: INK, bold: true } },
    { text: "   <   ", options: { color: GOLD, bold: true } },
    { text: "구독 3,900원", options: { color: INK, bold: true } },
    { text: "   =   마진 확보", options: { color: GOLD, bold: true } },
  ], { x: gx, y: gy + ch + 0.35, w: totalW, h: 0.85, fontFace: FONT, fontSize: 16, align: "center", valign: "middle", margin: 0 });
  footer(s, 7);
}

// ════════ 8. 로드맵 / 현황 ════════
{
  const s = p.addSlide();
  header(s, "지금 어디까지 왔나", "백엔드 두뇌는 완성 — 베타를 향해 가고 있습니다");
  const steps = [
    ["M1", "기반 구축", "성경 DB · 서버 · 파이프라인", 2],
    ["M2", "프롬프트 · QA", "5종 프롬프트 · 위기 · 검증", 1],
    ["M3", "클로즈드 베타", "교회 청년부 30명", 0],
    ["M4", "고도화", "피드백 · 리텐션 측정", 0],
    ["M5", "앱 · 구독", "RN 앱 · 구독 도입", 0],
  ];
  const n = steps.length, cw = 2.15, gy = 2.5, gapX = 0.32;
  const totalW = cw * n + gapX * (n - 1), gx = (W - totalW) / 2;
  // 연결선
  s.addShape(p.shapes.LINE, { x: gx + cw / 2, y: gy + 0.4, w: totalW - cw, h: 0, line: { color: LINE, width: 2 } });
  steps.forEach((st, i) => {
    const x = gx + i * (cw + gapX);
    const state = st[3]; // 2=완료, 1=진행, 0=예정
    const fill = state === 2 ? GOLD : state === 1 ? WHITE : MIST;
    const ringW = state === 1 ? 3 : 0;
    s.addShape(p.shapes.OVAL, { x: x + cw / 2 - 0.4, y: gy, w: 0.8, h: 0.8, fill: { color: fill }, line: state === 1 ? { color: GOLD, width: ringW } : { color: LINE, width: 1 } });
    s.addText(st[0], { x: x + cw / 2 - 0.4, y: gy, w: 0.8, h: 0.8, fontFace: FONT, fontSize: 16, bold: true, color: state === 2 ? WHITE : (state === 1 ? GOLD : SLATE), align: "center", valign: "middle", margin: 0 });
    s.addText(st[1], { x: x - 0.1, y: gy + 1.0, w: cw + 0.2, h: 0.45, fontFace: FONT, fontSize: 15, bold: true, color: INK, align: "center", margin: 0 });
    s.addText(st[2], { x: x - 0.1, y: gy + 1.45, w: cw + 0.2, h: 0.8, fontFace: FONT, fontSize: 11.5, color: SLATE, align: "center", lineSpacingMultiple: 1.15, margin: 0 });
    if (state > 0) {
      s.addText(state === 2 ? "완료" : "진행 중", { x: x + cw / 2 - 0.7, y: gy - 0.55, w: 1.4, h: 0.35, fontFace: FONT, fontSize: 11, bold: true, color: GOLD, align: "center", margin: 0 });
    }
  });
  s.addText("현재 — 백엔드 핵심 파이프라인 구현 완료, 단위 테스트 통과", { x: 0.7, y: 5.9, w: 12, h: 0.4, fontFace: FONT, fontSize: 14, italic: true, color: SLATE, align: "center", margin: 0 });
  footer(s, 8);
}

// ════════ 9. 비전 / 마무리 ════════
{
  const s = p.addSlide();
  s.background = { color: INK };
  s.addText("AI가 신앙을 대체하지 않습니다", { x: 0.95, y: 2.2, w: 11.5, h: 0.7, fontFace: FONT, fontSize: 22, color: GOLDLT, margin: 0 });
  s.addText("교회로 연결하는 다리가 됩니다", { x: 0.9, y: 2.95, w: 11.5, h: 1.0, fontFace: FONT, fontSize: 42, bold: true, color: WHITE, margin: 0 });
  s.addText("정죄 없이, 환각 없이, 안전하게 — 필요한 순간 말씀으로 동행하는 친구.", { x: 0.95, y: 4.25, w: 11, h: 0.6, fontFace: FONT, fontSize: 17, color: "C9D4E5", margin: 0 });
  s.addText("말씀벗", { x: 0.95, y: 5.5, w: 6, h: 0.7, fontFace: FONT, fontSize: 30, bold: true, color: GOLD, margin: 0 });
  s.addText("”", { x: 9.9, y: 2.6, w: 3, h: 3, fontFace: "Georgia", fontSize: 300, color: INK2, align: "center", valign: "middle", margin: 0 });
}

p.writeFile({ fileName: "말씀벗_피치덱.pptx" }).then((f) => console.log("created:", f));
