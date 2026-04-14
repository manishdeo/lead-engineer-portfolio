# 🔒 Security & Authentication Interview Questions (90+)

---

### Q1: How do you secure microservices communication?
**A:**
- **External → Gateway:** TLS, JWT authentication, rate limiting
- **Gateway → Services:** Internal network, mTLS (service mesh)
- **Service → Service:** mTLS or signed JWTs, network policies
- **Service → DB:** Encrypted connections, credential rotation (Vault)

### Q2: JWT vs Session-based auth — trade-offs?
**A:**
| Aspect | JWT | Session |
|--------|-----|---------|
| Stateless | ✅ Yes | ❌ Server stores session |
| Scalability | ✅ No shared state | ❌ Need sticky sessions or shared store |
| Revocation | ❌ Hard (blacklist needed) | ✅ Delete session |
| Size | ❌ Larger (payload in token) | ✅ Small session ID |
| Best for | Microservices, APIs | Traditional web apps |

### Q3: What is OWASP Top 10? Name the critical ones.
**A:**
1. **Broken Access Control** — Unauthorized access to resources
2. **Cryptographic Failures** — Weak encryption, exposed secrets
3. **Injection** — SQL, NoSQL, OS command injection
4. **Insecure Design** — Missing security controls by design
5. **Security Misconfiguration** — Default credentials, open ports
6. **Vulnerable Components** — Outdated libraries with known CVEs
7. **Authentication Failures** — Weak passwords, missing MFA
8. **Software Integrity Failures** — Unsigned updates, compromised CI/CD
9. **Logging Failures** — No audit trail
10. **SSRF** — Server-Side Request Forgery

### Q4: How do you prevent SQL injection?
**A:** Parameterized queries (JPA/Hibernate does this by default), input validation, least-privilege DB accounts, WAF rules. Never concatenate user input into SQL.

### Q5: What is Zero Trust security?
**A:** "Never trust, always verify." Every request is authenticated and authorized regardless of network location. No implicit trust for internal network. Principles: verify identity, least privilege, assume breach.

### Q6: How do you handle secrets management?
**A:**
- **Never** in code or config files
- Use: AWS Secrets Manager, HashiCorp Vault, K8s External Secrets
- Auto-rotation of credentials
- Audit access to secrets
- Encrypt at rest and in transit

### Q7: What is CORS? How do you configure it securely?
**A:** Cross-Origin Resource Sharing — browser security mechanism. Configure: whitelist specific origins (never `*` in production), limit methods and headers, set `credentials: true` only when needed.
