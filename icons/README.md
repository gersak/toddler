#### Show icons in file

```bash
sed -n '/(helix\.core\/defnc/ {n; s/^[[:space:]]*\([^[:space:]()]*\).*/\1/p;}' outlined.cljc
```
