# JSTech Prime

Site público da JSTech Prime para apresentar serviços de TV autorizada, planos, testes quando disponíveis, atendimento e suporte.

## Publicação no GitHub Pages

1. Abra **Settings > Pages** no repositório.
2. Em **Build and deployment**, selecione **GitHub Actions**.
3. Acesse a aba **Actions** e aguarde o workflow **Publicar JSTech Prime** terminar.
4. O endereço será: https://juliovianadeoliveira-source.github.io/jstech-prime-site/

O workflow já está em `.github/workflows/pages.yml` e publica a branch `main` automaticamente.

## Integração

O catálogo e os contatos usam as funções públicas do Supabase configuradas no projeto JSTech Prime. O formulário registra nome, WhatsApp, serviço e mensagem; o cadastro de novidades registra o consentimento no mesmo fluxo.

## Privacidade

A página usa `noindex,nofollow` e `robots.txt` bloqueado para evitar indexação direta. O cadastro de novidades exige consentimento. Para disparos automáticos de e-mail, conecte um provedor autorizado (SMTP, Resend ou similar) e respeite a opção de cancelamento do cliente.
