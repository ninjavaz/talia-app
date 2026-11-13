import { solveProofOfWork } from './proof-of-work';

async function sha256Hex(text: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('');
}

describe('solveProofOfWork', () => {
  it('finds the secret number', async () => {
    const salt = 'abc123?expires=9999999999';
    const challenge = await sha256Hex(salt + 377);
    const solution = await solveProofOfWork({
      algorithm: 'SHA-256',
      challenge,
      salt,
      maxNumber: 1000,
      signature: 'sig',
    });
    expect(solution.number).toBe(377);
    expect(solution.signature).toBe('sig');
  });
});
