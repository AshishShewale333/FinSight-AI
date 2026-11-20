DELETE FROM financial_news
WHERE source = 'DEMO PRACTICE'
   OR external_id LIKE 'finsight-demo-%';
