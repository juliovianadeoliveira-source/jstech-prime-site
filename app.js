function normalizeSources(list){return (Array.isArray(list)?list:[]).map(function(s){return Object.assign({},s,{plans:(s.plans||[]).map(function(p){var service=String(p.service||'IPTV').replace(/^CS Satélite$/,'TV Satélite').replace(/^CS NET$/,'TV Cabo');return Object.assign({},p,{service:service});})});});}
const API='https://fvttsguxeocisqvcrbqh.supabase.co/functions/v1/jstech-prime-public-api';
const WHATSAPP='5527997314781';
const fallback=[{source_key:'plenocs',name:'Pleno CS',plans:[{name:'Mensal',price:15,months:1,service:'TV Satélite'},{name:'Trimestral',price:30,months:3,service:'TV Satélite'},{name:'Semestral',price:40,months:6,service:'TV Satélite'},{name:'Anual',price:80,months:12,service:'TV Satélite'},{name:'Mensal',price:15,months:1,service:'TV Cabo'},{name:'Trimestral',price:30,months:3,service:'TV Cabo'},{name:'Semestral',price:40,months:6,service:'TV Cabo'},{name:'Anual',price:80,months:12,service:'TV Cabo'},{name:'Mensal',price:25,months:1,service:'IPTV'},{name:'Trimestral',price:65,months:3,service:'IPTV'},{name:'Semestral',price:120,months:6,service:'IPTV'},{name:'Anual',price:210,months:12,service:'IPTV'}],compatibility:['Smart TV','TV Box','Celular','Computador']},{source_key:'questbr',name:'Quest BR',plans:[{name:'Mensal',price:25,months:1,service:'IPTV'},{name:'Trimestral',price:65,months:3,service:'IPTV'},{name:'Semestral',price:130,months:6,service:'IPTV'},{name:'Anual',price:250,months:12,service:'IPTV'}],compatibility:['Android TV','Fire TV Stick','Chromecast','Roku','Android','iPhone','Notebook','PC','iPad','Apple TV']}];
let sources=fallback,csService='TV Satélite',iptvSource='plenocs';
const money=v=>Number(v||0).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2});
const uniq=a=>[...new Set(a.filter(Boolean))];
const fixedCsPlans=[{name:'Mensal',price:15,months:1},{name:'Trimestral',price:30,months:3},{name:'Semestral',price:40,months:6},{name:'Anual',price:80,months:12}];
const wa=text=>`https://wa.me/${WHATSAPP}?text=${encodeURIComponent(text)}`;

function wireWhatsApp(){document.querySelectorAll('[data-wa]').forEach(el=>{let text='Olá! Vim pelo site da JSTech Prime e gostaria de conhecer os serviços.';if(el.dataset.wa==='trial')text='Olá! Vim pelo site da JSTech Prime e gostaria de solicitar um teste.';if(el.dataset.wa==='cs-trial')text='Olá! Vim pelo site da JSTech Prime e quero solicitar um teste de TV autorizada. Meu receptor é: ';if(el.dataset.wa==='iptv-trial')text='Olá! Vim pelo site da JSTech Prime e quero solicitar um teste de IPTV. Meu aparelho é: ';if(el.dataset.wa==='support')text='Olá! Vim pelo site da JSTech Prime e preciso de ajuda para escolher entre TV autorizada e TV online.';el.href=wa(text);el.target='_blank';el.rel='noopener noreferrer'})}
function sourceName(source){return ({plenocs:'Pleno IPTV',questbr:'Quest IPTV'}[source?.source_key]||source?.name||'IPTV')}
function plansFor(service,sourceKey){if(service==='TV Satélite'||service==='TV Cabo')return fixedCsPlans.map(p=>Object.assign({},p,{service:service}));const all=sourceKey?sources.find(s=>s.source_key===sourceKey)?.plans||[]:sources.flatMap(s=>s.plans||[]);return all.filter(p=>(p.service||'IPTV')===service).sort((a,b)=>(a.months||1)-(b.months||1))}
function planCards(plans,kind,label){if(!plans.length)return'<div class="plan"><h3>Atualizando planos…</h3><p>O último catálogo válido será carregado em instantes.</p></div>';const max=Math.max(...plans.map(p=>Number(p.months)||1));return plans.map(p=>{const months=Number(p.months)||1,featured=months===max&&plans.length>1,eq=p.monthly_equivalent||Number(p.price)/months;const msg=`Olá! Vim pelo site da JSTech Prime e quero ${label}, plano ${p.name}, por R$ ${money(p.price)}.`;return `<article class="plan ${featured?'featured':''}"><small class="tag">${featured?'MELHOR CUSTO':months===1?'PARA COMEÇAR':'ECONOMIZE MAIS'}</small><h3>${p.name}</h3><div class="price"><span>R$</span><b>${money(p.price)}</b></div><div class="equivalent">${months>1?`equivale a R$ ${money(eq)} por mês`:'pagamento mensal'}</div><ul><li>Teste quando disponível</li><li>Ativação rápida</li><li>Suporte pelo WhatsApp</li><li>Sem fidelidade obrigatória</li></ul><a class="button ${kind==='cs'?'cs-button':'iptv-button'}" href="${wa(msg)}" target="_blank">ESCOLHER PLANO →</a></article>`}).join('')}
function renderCS(){const tabs=document.getElementById('csTabs');tabs.innerHTML=['TV Satélite','TV Cabo'].map(s=>`<button class="${s===csService?'active':''}" data-service="${s}">${s}</button>`).join('');tabs.querySelectorAll('button').forEach(b=>b.onclick=()=>{csService=b.dataset.service;renderCS()});document.getElementById('csPlans').innerHTML=planCards(plansFor(csService),'cs',csService)}
function renderIPTV(){const available=sources.filter(s=>(s.plans||[]).some(p=>(p.service||'IPTV')==='IPTV'));if(!available.some(s=>s.source_key===iptvSource))iptvSource=available[0]?.source_key;const tabs=document.getElementById('iptvTabs');tabs.innerHTML=available.map(s=>`<button class="${s.source_key===iptvSource?'active':''}" data-source="${s.source_key}">${sourceName(s)}</button>`).join('');tabs.querySelectorAll('button').forEach(b=>b.onclick=()=>{iptvSource=b.dataset.source;renderIPTV()});const source=available.find(s=>s.source_key===iptvSource);document.getElementById('iptvPlans').innerHTML=planCards(plansFor('IPTV',iptvSource),'iptv',`IPTV ${sourceName(source)}`)}
function renderDevices(){const requested=['Smart TV','TV Box','Celular','Computador','Android TV','Fire TV Stick','Chromecast','Roku','Android','iPhone','Tablet','Notebook'];const discovered=uniq(sources.flatMap(s=>s.compatibility||[]));const devices=uniq(requested.concat(discovered));const icons=['📺','▣','▯','⌨','◫','◇'];document.getElementById('deviceList').innerHTML=devices.slice(0,12).map((d,i)=>`<button type="button" class="device-card" data-device="${deviceSlug(d)}"><span class="device-symbol">${icons[i%icons.length]}</span><b>${d}</b><span class="device-sub">Compatível com IPTV</span><span class="device-link">Ver informações →</span></button>`).join('');const dates=sources.map(s=>new Date(s.fetched_at||0)).filter(d=>d.getTime()>0);if(dates.length)document.getElementById('lastUpdate').textContent='Atualizado: '+new Date(Math.max(...dates)).toLocaleString('pt-BR')}

const deviceDetails={
  'receptor-satelite':{kicker:'TV SATÉLITE',title:'Receptor Satélite',lead:'Indicado para quem utiliza um receptor compatível com serviço autorizado via satélite.',need:'Receptor compatível, antena instalada e sinal disponível na sua região.',how:'Informe a marca e o modelo. Nossa equipe confirma a compatibilidade e orienta a ativação.',service:'TV Satélite'},
  'receptor-cabo':{kicker:'TV CABO',title:'Receptor Cabo',lead:'Indicado para receptores compatíveis com serviço autorizado de TV a cabo.',need:'Receptor compatível e rede de cabo disponível na sua região.',how:'Envie a marca e o modelo do receptor para conferirmos a opção correta.',service:'TV Cabo'},
  'smart-tv':{kicker:'IPTV',title:'Smart TV',lead:'Assista diretamente na televisão conectada à internet.',need:'Smart TV compatível, internet estável e o aplicativo indicado.',how:'Receba a orientação do aplicativo, instale e informe os dados de acesso.',service:'IPTV'},
  'tv-box':{kicker:'IPTV',title:'TV Box',lead:'Transforme sua TV em uma central de entretenimento conectada.',need:'TV Box compatível, fonte de energia e internet por Wi-Fi ou cabo.',how:'Conecte o aparelho, instale o aplicativo indicado e siga a configuração.',service:'IPTV'},
  'celular':{kicker:'IPTV',title:'Celular',lead:'Leve sua programação com você usando o celular.',need:'Celular compatível, aplicativo indicado e conexão com a internet.',how:'Instale o aplicativo orientado pela equipe e configure seu acesso.',service:'IPTV'},
  'computador':{kicker:'IPTV',title:'Computador',lead:'Use seu computador para assistir com praticidade.',need:'Computador ou navegador compatível e internet estável.',how:'Receba a orientação de acesso e configure o aplicativo ou navegador.',service:'IPTV'},
  'android-tv':{kicker:'IPTV',title:'Android TV',lead:'Uma experiência de TV online integrada ao sistema Android TV.',need:'Android TV compatível, internet e o aplicativo indicado.',how:'Pesquise o aplicativo orientado, instale e configure seu acesso.',service:'IPTV'},
  'fire-tv-stick':{kicker:'IPTV',title:'Fire TV Stick',lead:'Tenha IPTV em uma TV usando o Fire TV Stick.',need:'Fire TV Stick configurado, TV com entrada HDMI e internet.',how:'Conecte o dispositivo, instale o aplicativo indicado e siga a orientação.',service:'IPTV'},
  'chromecast':{kicker:'IPTV',title:'Chromecast',lead:'Envie a reprodução do celular para a televisão.',need:'Chromecast configurado, celular compatível e mesma rede Wi-Fi.',how:'Abra o aplicativo no celular, conecte ao Chromecast e comece a assistir.',service:'IPTV'},
  'roku':{kicker:'IPTV',title:'Roku',lead:'Acesse sua TV online em um dispositivo Roku compatível.',need:'Roku configurado, TV com HDMI e conexão com a internet.',how:'Instale o aplicativo disponível e configure com a orientação da equipe.',service:'IPTV'},
  'android':{kicker:'IPTV',title:'Android',lead:'Use seu aparelho Android para acessar o serviço online.',need:'Celular ou tablet Android compatível e conexão com a internet.',how:'Instale o aplicativo indicado e informe os dados recebidos.',service:'IPTV'},
  'iphone':{kicker:'IPTV',title:'iPhone',lead:'Assista pelo iPhone com um aplicativo compatível.',need:'iPhone compatível, aplicativo indicado e internet.',how:'Instale o aplicativo orientado e configure seu acesso com a equipe.',service:'IPTV'},
  'tablet':{kicker:'IPTV',title:'Tablet',lead:'Uma tela maior para assistir com conforto em qualquer lugar.',need:'Tablet compatível, aplicativo indicado e internet.',how:'Instale o aplicativo e siga os passos de configuração enviados.',service:'IPTV'},
  'notebook':{kicker:'IPTV',title:'Notebook',lead:'Assista pelo notebook em casa ou durante uma viagem.',need:'Notebook ou navegador compatível e conexão com a internet.',how:'Receba o acesso, abra no aplicativo ou navegador indicado e configure.',service:'IPTV'}
};
function deviceSlug(value){return String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'')}
function wireDeviceCards(){
  var modal=document.getElementById('deviceModal');if(!modal)return;
  var closeButton=document.getElementById('deviceModalClose'),lastFocus=null;
  function closeModal(){modal.hidden=true;modal.setAttribute('aria-hidden','true');document.body.classList.remove('modal-open');if(lastFocus)lastFocus.focus()}
  function openModal(key,card){
    var detail=deviceDetails[key]||{kicker:'COMPATIBILIDADE',title:(card.querySelector('b')||{}).textContent||'Aparelho',lead:'Confira as informações e peça ajuda para confirmar a compatibilidade.',need:'Aparelho compatível e conexão adequada ao serviço.',how:'Envie o modelo para nossa equipe orientar a configuração.',service:'IPTV'};
    document.getElementById('deviceModalKicker').textContent=detail.kicker;
    document.getElementById('deviceModalTitle').textContent=detail.title;
    document.getElementById('deviceModalLead').textContent=detail.lead;
    document.getElementById('deviceModalNeed').textContent=detail.need;
    document.getElementById('deviceModalHow').textContent=detail.how;
    var link=document.getElementById('deviceModalWhatsapp');
    link.href=wa('Olá! Vim pelo site da JSTech Prime e quero confirmar a compatibilidade do meu aparelho: '+detail.title+'.');
    modal.hidden=false;modal.setAttribute('aria-hidden','false');document.body.classList.add('modal-open');lastFocus=card;setTimeout(function(){closeButton.focus()},0);
  }
  document.querySelectorAll('.device-card').forEach(function(card){card.addEventListener('click',function(){openModal(card.dataset.device,card)});card.addEventListener('keydown',function(e){if(e.key==='Enter'||e.key===' '){e.preventDefault();openModal(card.dataset.device,card)}})});
  closeButton&&closeButton.addEventListener('click',closeModal);
  modal.querySelectorAll('[data-device-close]').forEach(function(el){el.addEventListener('click',closeModal)});
  document.addEventListener('keydown',function(e){if(e.key==='Escape'&&!modal.hidden)closeModal()});
}
async function load(){try{const r=await fetch(API,{cache:'no-store'});if(!r.ok)throw 0;const p=await r.json();if(Array.isArray(p.sources)&&p.sources.length)sources=normalizeSources(p.sources)}catch(e){console.warn('Catálogo de segurança ativo')}renderCS();renderIPTV();renderDevices();wireDeviceCards()}
document.querySelectorAll('.faq button').forEach(btn=>btn.onclick=()=>{const item=btn.closest('.faq'),open=item.classList.contains('open');document.querySelectorAll('.faq.open').forEach(x=>x.classList.remove('open'));if(!open)item.classList.add('open')});
const form=document.getElementById('leadForm');form?.addEventListener('submit',async e=>{e.preventDefault();const status=document.getElementById('formStatus'),button=form.querySelector('button');const data=Object.fromEntries(new FormData(form).entries());if(String(data.nome||'').trim().length<2||String(data.whatsapp||'').replace(/\D/g,'').length<8||!data.servico){status.textContent='Preencha nome, WhatsApp e serviço.';return}button.disabled=true;button.textContent='ENVIANDO…';try{const r=await fetch(API,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});if(!r.ok)throw 0;status.textContent='Solicitação enviada com sucesso!';form.reset()}catch{status.textContent='Não foi possível enviar. Use o WhatsApp.'}finally{button.disabled=false;button.textContent='ENVIAR SOLICITAÇÃO →'}});
wireWhatsApp();load();

(function(){
  var toggle=document.getElementById('chatToggle'), panel=document.getElementById('chatPanel'), close=document.getElementById('chatClose');
  var messages=document.getElementById('chatMessages'), form=document.getElementById('chatForm'), input=document.getElementById('chatInput');
  if(!toggle||!panel||!close||!messages||!form||!input)return;
  if(panel.dataset.chatBound==='true')return;
  panel.dataset.chatBound='true';
  var manuallyClosed=false;
  function add(text,kind){var el=document.createElement('div');el.className='chat-bubble '+kind;el.textContent=text;messages.appendChild(el);messages.scrollTop=messages.scrollHeight;return el}
  function openChat(){manuallyClosed=false;panel.hidden=false;panel.removeAttribute('hidden');toggle.setAttribute('aria-expanded','true')}
  function closeChat(){manuallyClosed=true;panel.hidden=true;panel.setAttribute('hidden','');toggle.setAttribute('aria-expanded','false')}
  function typeBot(text){
    var bubble=add('','bot');
    var chars=Array.from(String(text||'')),index=0;
    var timer=setInterval(function(){bubble.textContent+=chars[index++]||'';messages.scrollTop=messages.scrollHeight;if(index>=chars.length)clearInterval(timer)},24);
    return bubble;
  }
  function reply(text){
    var raw=String(text||'').trim(),t=raw.toLowerCase(),answer='';
    var greeting=/^(oi|olá|ola|bom dia|boa tarde|boa noite|e aí|e ai)\b/.test(t);
    var recommendation=/(indica|indico|recomenda|recomendo|melhor|qual escolher|o que.*(serve|indica)|sugere|sugestão|sugestao)/.test(t);
    var iptvDevice=/(iptv|smart tv|tv box|celular|computador|android tv|fire tv|chromecast|roku|iphone|tablet|notebook|internet)/.test(t);
    var receiver=/(satélite|satelite|cabo|receptor|antena)/.test(t);
    if(greeting&&raw.split(/\s+/).length<=4)answer='Oi! Como posso ajudar você hoje? Você pode perguntar sobre planos, testes, aparelhos ou configuração.';
    else if(recommendation&&!iptvDevice&&!receiver)answer='Eu indico assim: se você usa Smart TV, TV Box, celular, tablet ou computador com internet, o IPTV é o mais prático. Se já usa receptor com antena ou cabo, escolha TV Satélite/Cabo. Qual aparelho você tem?';
    else if(receiver){answer='Para receptor com antena ou cabo, a opção indicada é TV Satélite/Cabo. Me diga se o seu receptor é de satélite ou de cabo para eu orientar melhor.'}
    else if(iptvDevice){answer='Para '+(raw||'esse aparelho')+', a opção indicada é IPTV. Ele funciona em aparelhos conectados à internet. Quer ver os planos de IPTV ou saber como configurar?'}
    else if(t==='planos'||t.indexOf('plano')>=0)answer='Temos duas áreas: IPTV para aparelhos com internet e TV Satélite/Cabo para receptores compatíveis. Diga qual aparelho você usa que eu te mostro a opção certa.';
    else if(t.indexOf('teste')>=0)answer='O teste depende da disponibilidade e do aparelho. Diga o modelo que você usa e eu explico qual teste solicitar.';
    else if(t.indexOf('suporte')>=0||t.indexOf('ajuda')>=0)answer='Claro. Me diga o modelo do aparelho e o que você precisa fazer. Vou te orientar passo a passo por aqui.';
    else if(t.indexOf('instala')>=0||t.indexOf('config')>=0)answer='Eu te ajudo na configuração. Primeiro me diga o aparelho: Smart TV, TV Box, celular, computador ou receptor.';
    else if(t.indexOf('preço')>=0||t.indexOf('preco')>=0||t.indexOf('valor')>=0)answer='Os valores ficam separados por serviço. Diga se você quer IPTV ou TV Satélite/Cabo e qual aparelho usa para eu indicar a tabela correta.';
    else if(t.indexOf('humano')>=0||t.indexOf('pessoa')>=0||t.indexOf('whatsapp')>=0)answer='Posso continuar o atendimento por aqui. Me explique o que você precisa e eu vou te orientando.';
    else answer='Para te indicar corretamente, me diga duas coisas: qual aparelho você usa e se quer assistir pela internet ou por receptor.';
    var typing=add('Digitando...','bot typing');setTimeout(function(){if(typing.parentNode)typing.remove();typeBot(answer)},520);
  }
  openChat();
  toggle.addEventListener('click',function(e){e.preventDefault();e.stopPropagation();panel.hidden?openChat():closeChat()});
  close.addEventListener('click',function(e){e.preventDefault();e.stopPropagation();closeChat()});
  document.querySelectorAll('[data-chat]').forEach(function(b){b.addEventListener('click',function(e){e.preventDefault();openChat();add(b.textContent,'user');reply(b.dataset.chat||b.textContent)})});
  form.addEventListener('submit',function(e){e.preventDefault();var text=input.value.trim();if(!text)return;add(text,'user');input.value='';reply(text)});
  document.addEventListener('keydown',function(e){if(e.key==='Escape'&&!panel.hidden)closeChat()});
})();
(function(){
  var PROMO_API='https://fvttsguxeocisqvcrbqh.supabase.co/functions/v1/jstech-prime-site';
  var form=document.getElementById('promoForm');if(!form)return;var status=document.getElementById('promoStatus');
  form.addEventListener('submit',function(e){e.preventDefault();var email=(new FormData(form).get('email')||'').toString().trim().toLowerCase();if(!email){status.textContent='Informe um e-mail válido.';return}var button=form.querySelector('button');button.disabled=true;button.textContent='CADASTRANDO...';status.textContent='';
    fetch(PROMO_API,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({nome:'Lista de promoções',whatsapp:'email:'+email,servico:'Promoções',mensagem:'Consentimento confirmado para receber novidades.'})}).then(function(r){if(!r.ok)throw 0;return r.json()}).then(function(){status.textContent='Cadastro recebido. Você só receberá mensagens com seu consentimento.';form.reset()}).catch(function(){status.textContent='Não foi possível cadastrar agora. Tente novamente ou fale pelo WhatsApp.'}).finally(function(){button.disabled=false;button.textContent='QUERO RECEBER →'});
  });
})();