const { db } = require('../../shared/src/dynamodb');

/**
 * Daily Reconciliation — Compare internal ledger with bank records.
 *
 * Triggered by EventBridge scheduled rule (daily at 2 AM UTC).
 * Scans all payments from previous day, verifies ledger balance.
 */
exports.handler = async (event) => {
  console.log('Starting daily reconciliation...');

  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0];
  let totalDebits = 0;
  let totalCredits = 0;
  let discrepancies = [];

  // In production: scan/query payments from yesterday
  // For demo: verify ledger balance principle
  try {
    // Simulate reconciliation
    const report = {
      date: yesterday,
      totalPayments: 0,
      totalAuthorized: 0,
      totalCaptured: 0,
      totalRefunded: 0,
      ledgerBalanced: totalDebits === totalCredits,
      discrepancies,
      completedAt: new Date().toISOString(),
    };

    // Store reconciliation report
    await db.putItem({
      PK: `RECON#${yesterday}`,
      SK: 'REPORT',
      ...report,
    });

    if (discrepancies.length > 0) {
      console.warn(`Reconciliation found ${discrepancies.length} discrepancies`);
      // In production: trigger alert via SNS
    }

    console.log(`Reconciliation complete for ${yesterday}`);
    return report;
  } catch (err) {
    console.error('Reconciliation failed:', err);
    throw err;
  }
};
