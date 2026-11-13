export interface PowChallenge {
  algorithm: string;
  challenge: string;
  salt: string;
  maxNumber: number;
  signature: string;
}

export interface PowSolution {
  algorithm: string;
  challenge: string;
  salt: string;
  number: number;
  signature: string;
}

/**
 * Solves the server's proof-of-work: finds n with SHA-256(salt + n) == challenge.
 * Uses the browser's native WebCrypto; each digest is awaited, so the UI stays responsive.
 * Takes about a second on a phone, which is nothing for a person and expensive for a bot farm.
 */
export async function solveProofOfWork(c: PowChallenge): Promise<PowSolution> {
  const encoder = new TextEncoder();
  for (let n = 0; n <= c.maxNumber; n++) {
    const digest = await crypto.subtle.digest('SHA-256', encoder.encode(c.salt + n));
    if (toHex(digest) === c.challenge) {
      return { algorithm: c.algorithm, challenge: c.challenge, salt: c.salt, number: n, signature: c.signature };
    }
  }
  throw new Error('Proof of work could not be solved');
}

function toHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer), (b) => b.toString(16).padStart(2, '0')).join('');
}
