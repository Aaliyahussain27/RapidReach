import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://fsuttyjfqfiwpsmdenff.supabase.co';
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZzdXR0eWpmcWZpd3BzbWRlbmZmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ1NDYwMDIsImV4cCI6MjA5MDEyMjAwMn0.7RD1B7fSRb8g4cyZQK2vKFpm0iqmMOOJN40rdQkFCdA';

export const supabase = createClient(supabaseUrl, supabaseKey);
