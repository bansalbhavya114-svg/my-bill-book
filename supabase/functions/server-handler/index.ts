// Supabase Edge Function Handler for BillBook
// Runtimes: Supabase Edge Functions (Deno) / Node.js Backend with @supabase/server
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  // Handle CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "https://srpuwmqmxvwjdyyjgueh.supabase.co";
    const supabaseSecretKey = Deno.env.get("SUPABASE_SECRET_KEY");

    if (!supabaseSecretKey) {
      throw new Error("SUPABASE_SECRET_KEY is not configured in Edge Function secrets.");
    }

    // Initialize privileged admin client for server-side operations
    const supabaseAdmin = createClient(supabaseUrl, supabaseSecretKey);

    // Verify incoming user session via Authorization header
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: "Missing Authorization header" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const token = authHeader.replace("Bearer ", "").trim();
    const { data: { user }, error: authError } = await supabaseAdmin.auth.getUser(token);

    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: "Invalid or expired user session token" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body = await req.json().catch(() => ({}));
    const { action = "ping" } = body;

    return new Response(
      JSON.stringify({
        status: "ok",
        action,
        userId: user.id,
        userEmail: user.email,
        timestamp: new Date().toISOString(),
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error: any) {
    return new Response(
      JSON.stringify({ error: error.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
