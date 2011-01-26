require 'detective.rb'
require 'mediawiki_api.rb'
require 'uri'
require 'cgi'
require 'net/http'
require 'alexa_config.rb'
require 'alexa_urlinfo.rb'
require 'base64'
require 'bundler/setup'
require 'nokogiri'

class ExternalLinkDetective < Detective
  def self.table_name
    'link'
  end

  #return a proc that defines the columns used by this detective
  #if using this as an example, you probably should copy the first two columns (the id and foreign key)
  def self.columns
    Proc.new do
      id integer primary key autoincrement,
      revision_id integer,                              --foreign key to reference the original revision
      http_response boolean,
      link string,

      source text,
      site_description string,

      title string,
      description string, 
      online_since timestamp,
      speed_medianloadtime decimal, 
      speed_percentile decimal,
      adult_content integer,
      language_locale string,
      language_encoding string,
      links_in_count integer,
      keywords string,
      num_related_links integer,
      related_links string,

      rank integer,
      rank_delta decimal,
      reach_rank integer, 
      reach_rank_delta decimal,
      reach_permill decimal, 
      reach_permill_delta decimal,
      views_permill decimal,
      views_permill_delta decimal,
      views_rank integer,
      views_rank_delta decimal,
      views_peruser decimal, 
      views_peruser_delta decimal,

      rank_by_city string,
      rank_by_country string,

      FOREIGN KEY(revision_id) REFERENCES irc_wikimedia_org_en_wikipedia(id)  
    end
  end

  #info is a list: 
  # 0: sample_id (string), 
  # 1: article_name (string), 
  # 2: desc (string), 
  # 3: rev_id (string),
  # 4: old_id (string)
  # 5: user (string), 
  # 6: byte_diff (int), 
  # 7: timestamp (Time object), 
  # 8: description (string)
  def investigate info
        
    linkarray = find_link_info(info)
    
    linkarray.each do |linkentry|
      rownum = db_write!(
        ['revision_id', 'link', 'source', 'site_description', 'title', 'description', 'online_since', 'speed_medianloadtime', 'speed_percentile', 'adult_content', 'language_locale',
      'language_encoding',
      'links_in_count',
      'keywords',
      'num_related_links',
      'related_links',
      'rank',
      'rank_delta',
      'reach_rank', 
      'reach_rank_delta',
      'reach_permill', 
      'reach_permill_delta',
      'views_permill',
      'views_permill_delta',
      'views_rank',
      'views_rank_delta','views_peruser','views_peruser_delta',
      'rank_by_city',
      'rank_by_country'],
	      [info[0], linkentry["link"], linkentry["source"], linkentry["description"]] + linkentry["linkinfo"]    )
    end	
    true
  end	
  
  #really only uses revid and previous
  def find_link_info info
    #this is actually 'page' stuff
    #take popularity from: http://www.trendingtopics.org/page/[article_name]; links to csv's with daily and hourly popularity
    #http://stats.grok.se/en/top <- lists top pages
    #http://stats.grok.se/en/[year][month]/[article_name]
    #also http://toolserver.org/~emw/wikistats/?p1=Barack_Obama&project1=en&from=12/10/2007&to=12/11/2010&plot=1
    #http://wikitech.wikimedia.org/view/Main_Page
    #http://lists.wikimedia.org/pipermail/wikitech-l/2007-December/035435.html
    #http://wiki.wikked.net/wiki/Wikimedia_statistics/Daily
    #http://aws.amazon.com/datasets/Encyclopedic/4182
    #https://github.com/datawrangling/trendingtopics

    #link popularity/safety stuff:
    #http://code.google.com/apis/safebrowsing/
    #http://groups.google.com/group/google-safe-browsing-api/browse_thread/thread/b711ba69a4ecbb2f/29aa959a3a28a0bd?#29aa959a3a28a0bd

    #this is what we're going to do: get all external links for prev_id and all external links for curr_id and diff them, any added => new extrnal links to find
    #http://en.wikipedia.org/w/api.php?action=query&prop=extlinks&revids=800129
    #http://en.wikipedia.org/w/api.php?action=query&prop=extlinks&revids=409897423&ellimit=500
    #http://en.wikipedia.org/w/api.php?action=query&prop=extlinks&revids=409897009&ellimit=500
    #diff text: http://en.wikipedia.org/w/api.php?action=query&prop=revisions&revids=409897423&rvdiffto=prev
    xml = get_xml({:format => :xml, :action => :query, :prop => :revisions, :revids => info[3], :rvdiffto => 'prev'})
    diff_text = Nokogiri.XML(xml).css('diff').children.to_s
    diff_html = CGI.unescapeHTML(diff_text)
    noked = Nokogiri.HTML(diff_html)
    
    #TODO can have bad revid's (ie first edits on a page)
    linkarray = []
    noked.css('.diff-addedline').each do |td| #TODO should probably be looking specifically at .diffchange children for added text within the line
      revision_line = Nokogiri.HTML(CGI.unescapeHTML(td.children.to_s)).css('div').children
      #http://daringfireball.net/2010/07/improved_regex_for_matching_urls
      #%r{(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))}
      url = %r{(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))}
      #based on http://www.mediawiki.org/wiki/Markup_spec/BNF/Links
      external_link_regex = /\[(#{url}\s*(.*?))\]/
      #TODO pull any correctly formed links too?
      res = revision_line.to_s.scan(external_link_regex)
      if res.size > 0
        #p res
        res = res.first.compact
        #["http://www.eyemagazine.com/feature.php?id=62&amp;fid=270 Designing heroes", "http://www.eyemagazine.com/feature.php?id=62&amp;fid=270", "Designing heroes"]
        linkarray << [res[1], #link
                      res[2]] #description
      end
    end
    
    ret = []
    linkarray.each do |arr|
      source, success = find_source(arr.first)
      linkinfo = find_alexa_info(arr.first)

      ret << {"link" => arr.first, "source" => source, "http_response" => success, 'description' => arr.last, "linkinfo" => linkinfo}
    end
    ret
  end
  
  def find_source(url)
    #TODO do a check for the size and type-content of it before we pull it
    #binary files we probably don't need to grab and things larger than a certain size we don't want to grab
    uri = URI.parse(url)
    
    http = Net::HTTP.new(uri.host)
    resp = nil
    begin
      path = uri.path.to_s.empty? ? '/' : "#{uri.path}?#{uri.query}"
      resp = http.request_get(path, 'User-Agent' => 'WikipediaAntiSpamBot/0.1 (+hincapie.cis.upenn.edu)')
    rescue SocketError => e
      resp = e
    end
    
    ret = []
    if(resp.is_a? Net::HTTPOK or resp.is_a? Net::HTTPFound)
      if resp.content_type == 'text/html'
        #puts resp.body.length
        ret << resp.body[0..10**5] #truncate at 100K characters; not a good way to deal with size, should check the headers only
      else
        ret << resp.content_type
      end
      ret << true
    else
      ret << resp.class.to_s
      ret << false
    end
    ret
    # response = Net::HTTP.get_response(URI.parse(uri_str))
    # case response
    # when Net::HTTPSuccess     then response
    # when Net::HTTPRedirection then fetch(response['location'], limit - 1)
    # else
    #   response.error!
    # end
    
    #response = nil
    #Net::HTTP.start('some.www.server', 80) {|http|
    #  response = http.head('/index.html')
    #}
    #p response['content-type']
  end
  
def find_alexa_info(link)
      Alexa.config do |c|
          c.access_key_id = ALEXA_KEY_ID
	  c.secret_access_key = ALEXA_SECRET_KEY
      end
      linkinfo = Alexa::UrlInfo.new(:host => link)
      xml = linkinfo.connect
      linkinfo.parse_xml(xml)
       rank, rank_delta, reach_rank, reach_rank_delta, reach_permill, reach_permill_delta, views_permill, views_permill_delta, views_rank, views_rank_delta, views_peruser, views_peruser_delt = nil
      stats = linkinfo::usage_statistics
      if stats
      	 if !stats.empty?
            if !stats.first['Rank'].empty?
                rank = stats.first['Rank']['Value']
		rank_delta = stats.first['Rank']['Delta']
            end
            if !stats.first['Reach'].empty?
	       if !stats.first['Reach']['Rank'].empty?
                reach_rank = stats.first['Reach']['Rank']['Value']
		reach_rank_delta = stats.first['Reach']['Rank']['Delta']
               end
	       if !stats.first['Reach']['PerMillion'].empty?
                reach_permill = stats.first['Reach']['PerMillion']['Value']
		rank_permill_delta = stats.first['Reach']['PerMillion']['Delta']
               end	       
	    end       
            if !stats.first['PageViews'].empty?
	       if !stats.first['PageViews']['Rank'].empty?
                views_rank = stats.first['PageViews']['Rank']['Value']
		views_rank_delta = stats.first['PageViews']['Rank']['Delta']
               end
	       if !stats.first['PageViews']['PerMillion'].empty?
                views_permill = stats.first['PageViews']['PerMillion']['Value']
		views_permill_delta = stats.first['PageViews']['PerMillion']['Delta']
	       end
	       if !stats.first['PageViews']['PerUser'].empty?
                views_peruser = stats.first['PageViews']['PerUser']['Value']
		views_peruser_delta = stats.first['PageViews']['PerUser']['Delta']
               end	       
	    end      
         end
      end
      
      if linkinfo::adult_content == "no"
         ac = 0
      else
         ac = 1
      end

      [linkinfo::site_title.to_s, linkinfo::site_description.to_s, Time.parse(linkinfo::online_since).to_i, linkinfo::speed_median_load_time.to_f, linkinfo::speed_percentile.to_f, ac, linkinfo::language_locale.to_s, linkinfo::language_encoding.to_s, linkinfo::links_in_count.to_i, Base64.encode64(Marshal.dump(linkinfo::keywords)), linkinfo::related_links.size.to_i, Base64.encode64(Marshal.dump(linkinfo::related_links)), rank.to_i, rank_delta.to_f, reach_rank.to_i, reach_rank_delta.to_f, reach_permill.to_f, reach_permill_delta.to_f, views_permill.to_f, views_permill_delta.to_f, views_rank.to_i, views_rank_delta.to_f, views_peruser.to_f, views_peruser_delta.to_f, Base64.encode64(Marshal.dump(linkinfo::rank_by_city)), Base64.encode64(Marshal.dump(linkinfo::rank_by_country))]
  end
end
