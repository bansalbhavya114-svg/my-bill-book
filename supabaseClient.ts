/**
 * Supabase client configuration for TypeScript / Node.js runtimes.
 * Uses SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY from .env
 */
import { createClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.SUPABASE_URL || "https://srpuwmqmxvwjdyyjgueh.supabase.co";
const supabasePublishableKey = process.env.SUPABASE_PUBLISHABLE_KEY || "sb_publishable_L_rJeJgFezV1cXJevYtBgw_IsfMNZRk";

export const supabase = createClient(supabaseUrl, supabasePublishableKey);
