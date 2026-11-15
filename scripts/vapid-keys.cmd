@echo off
rem Prints a new VAPID key pair for Web Push (environment variables VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY).
rem Uses only Node's built-in crypto. Keep the private key secret (never commit it).
node -e "const {generateKeyPairSync}=require('crypto');const k=generateKeyPairSync('ec',{namedCurve:'prime256v1'}).privateKey.export({format:'jwk'});const b=s=>Buffer.from(s,'base64url');console.log('VAPID_PUBLIC_KEY='+Buffer.concat([Buffer.from([4]),b(k.x),b(k.y)]).toString('base64url'));console.log('VAPID_PRIVATE_KEY='+k.d)"
