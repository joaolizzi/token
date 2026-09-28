# Token - protótipo de assinatura e validação

Primeiro MVP para testar o fluxo:

1. uma requisição é criada para um usuário específico;
2. o servidor gera um token aleatório exclusivo daquela requisição;
3. somente o usuário associado + token correto conseguem assinar;
4. o token deixa de ser aceito depois da assinatura;
5. a assinatura gera um código público de validação;
6. esse código possui um QR Code que leva ao endpoint de validação do documento.

> Neste protótipo os dados ficam somente em memória. Ao reiniciar a aplicação, as requisições são apagadas.
>
> O endpoint de criação devolve o token no JSON APENAS para facilitar os testes locais. Na versão real, o token deverá ser entregue somente ao usuário destinatário.

## Requisitos

- Java 21
- Maven

## Rodar

```bash
mvn spring-boot:run
```

Servidor: `http://localhost:8080`

## 1. Criar requisição

```bash
curl -X POST http://localhost:8080/api/requests \
  -H "Content-Type: application/json" \
  -d '{"userId":"42","documentName":"contrato.pdf","documentHash":"SHA256-DO-DOCUMENTO"}'
```

Resposta aproximada:

```json
{
  "requestId": "UUID-DA-REQUISICAO",
  "userId": "42",
  "documentName": "contrato.pdf",
  "status": "PENDING",
  "expiresAt": "...",
  "token": "TOKEN-SECRETO-DO-TESTE",
  "warning": "TESTE: o token aparece nesta resposta apenas durante o protótipo"
}
```

Crie duas requisições para o mesmo `userId`: os tokens devem ser diferentes.

## 2. Tentar assinar como usuário errado

Substitua `REQUEST_ID` e `TOKEN` pelos valores retornados acima:

```bash
curl -X POST http://localhost:8080/api/requests/REQUEST_ID/sign \
  -H "Content-Type: application/json" \
  -d '{"userId":"99","token":"TOKEN"}'
```

Esperado: HTTP `403 Forbidden`.

## 3. Tentar token errado

```bash
curl -X POST http://localhost:8080/api/requests/REQUEST_ID/sign \
  -H "Content-Type: application/json" \
  -d '{"userId":"42","token":"ERRADO"}'
```

Esperado: HTTP `401 Unauthorized`.

## 4. Assinar corretamente

```bash
curl -X POST http://localhost:8080/api/requests/REQUEST_ID/sign \
  -H "Content-Type: application/json" \
  -d '{"userId":"42","token":"TOKEN"}'
```

A resposta terá `status: SIGNED` e um `validationCode`.

## 5. Tentar usar o mesmo token outra vez

Repita a chamada anterior.

Esperado: HTTP `409 Conflict`.

## 6. Validar documento

```bash
curl http://localhost:8080/api/validation/VALIDATION_CODE
```

Resposta aproximada:

```json
{
  "valid": true,
  "status": "VALID",
  "validationCode": "...",
  "userId": "42",
  "documentName": "contrato.pdf",
  "documentHash": "SHA256-DO-DOCUMENTO",
  "signedAt": "..."
}
```

## 7. Abrir o QR Code

No navegador:

```text
http://localhost:8080/api/validation/VALIDATION_CODE/qr
```

O QR contém a URL pública de validação do documento.

## Segurança já simulada no MVP

- token aleatório gerado com `SecureRandom`;
- token diferente para cada requisição;
- token associado a um usuário e documento;
- somente o hash SHA-256 do token fica armazenado internamente;
- comparação de hash com `MessageDigest.isEqual`;
- expiração em 10 minutos;
- requisição de uso único;
- QR final usa outro código público, e não o token secreto de assinatura.

## Próxima etapa

Depois que o fluxo estiver validado, substituir o armazenamento em memória por PostgreSQL/Supabase, autenticar o usuário de verdade (JWT/sessão) e fazer o `userId` vir da sessão autenticada em vez do corpo da requisição.
