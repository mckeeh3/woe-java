// ==UserScript==
// @name         Map dark
// @namespace    SH-Play Games
// @version      0.3
// @description  Makes the map dark
// @author       SanniHameln
// @include      /^https?:\/\/[www.]*(?:leitstellenspiel\.de|missionchief\.co\.uk|missionchief\.com|meldkamerspel\.com|centro-de-mando\.es|missionchief-australia\.com|larmcentralen-spelet\.se|operatorratunkowy\.pl|operatore112\.it|operateur112\.fr|dispetcher112\.ru|alarmcentral-spil\.dk|nodsentralspillet\.com|operacni-stredisko\.cz|jogo-operador112\.com|operador193\.com|dyspetcher101-game\.com|missionchief-japan\.com|missionchief-korea\.com|jocdispecerat112\.com|hatakeskuspeli\.com|dispecerske-centrum\.com)\/.*$/
// @grant        GM_addStyle
// ==/UserScript==

GM_addStyle(`
    .leaflet-tile {
    filter:invert(1) grayscale(.5);
    -webkit-filter:hue-rotate(180deg) invert(100%);
`);
