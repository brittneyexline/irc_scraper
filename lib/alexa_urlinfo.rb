require "cgi"
require "base64"
require "openssl"
require "digest/sha1"


module Alexa
  class UrlInfo
    RESPONSE_GROUP = "Rank,ContactInfo,AdultContent,Speed,Language,Keywords,OwnedDomains,LinksInCount,SiteData,RelatedLinks,RankByCountry,RankByCity,UsageStats"
    attr_accessor :host, :response_group, :xml_response,
      :rank, :data_url, :site_title, :site_description, :language_locale, :language_encoding,
      :links_in_count, :keywords, :related_links, :speed_median_load_time, :speed_percentile,
      :rank_by_country, :rank_by_city, :usage_statistics, :adult_content, :online_since

    def initialize(options = {})
      Alexa.config.access_key_id = options[:access_key_id] || Alexa.config.access_key_id
      Alexa.config.secret_access_key = options[:secret_access_key] || Alexa.config.secret_access_key
      raise ArgumentError.new("you must specify access_key_id") if Alexa.config.access_key_id.nil?
      raise ArgumentError.new("you must specify secret_access_key") if Alexa.config.secret_access_key.nil?
      @host = options[:host] or raise ArgumentError.new("you must specify host")
      @response_group = options[:response_group] || RESPONSE_GROUP
    end

    def connect
        action = "UrlInfo"
        timestamp = Time::now.utc.strftime("%Y-%m-%dT%H:%M:%S.000Z")
      	signature = generate_signature(Alexa.config.secret_access_key, action, timestamp)
      	url = generate_url(action, Alexa.config.access_key_id, signature, timestamp, response_group, host)
      	response = Net::HTTP.start(url.host) do |http|
          http.get url.request_uri
        end
      	@xml_response = handle_response(response).body
    end

    def parse_xml(xml)
      xml = XmlSimple.xml_in(force_encoding(xml), 'ForceArray' => false)
      group = response_group.split(',')

      alexa = xml['Response']['UrlInfoResult']['Alexa']
      @rank = alexa['TrafficData']['Rank'].to_i if group.include?('Rank') and !alexa['TrafficData']['Rank'].empty?
      @data_url = alexa['TrafficData']['DataUrl']['content'] if group.include?('Rank')
      @rank_by_country = alexa['TrafficData']['RankByCountry']['Country'] if group.include?('RankByCountry')
      @rank_by_city = alexa['TrafficData']['RankByCity']['City'] if group.include?('RankByCity')
      @usage_statistics = alexa['TrafficData']['UsageStatistics']["UsageStatistic"] if group.include?('UsageStats') and !alexa['TrafficData']['UsageStatistics'].nil?

      @site_title = alexa['ContentData']['SiteData']['Title'] if group.include?('SiteData')
      @site_description = alexa['ContentData']['SiteData']['Description'] if group.include?('SiteData')
      @language_locale = alexa['ContentData']['Language']['Locale'] if group.include?('Language')
      @language_encoding = alexa['ContentData']['Language']['Encoding'] if group.include?('Language')
      @online_since = alexa['ContentData']['SiteData']['OnlineSince'] if group.include?('SiteData')
      @links_in_count = alexa['ContentData']['LinksInCount'].to_i if group.include?('LinksInCount') and !alexa['ContentData']['LinksInCount'].empty?
      @keywords = alexa['ContentData']['Keywords']['Keyword'] if group.include?('Keywords')
      @speed_median_load_time = alexa['ContentData']['Speed']['MedianLoadTime'].to_i if group.include?('Speed') and !alexa['ContentData']['Speed']['MedianLoadTime'].empty?
      @speed_percentile = alexa['ContentData']['Speed']['Percentile'].to_i if group.include?('Speed') and !alexa['ContentData']['Speed']['Percentile'].empty?

      @related_links = alexa['Related']['RelatedLinks']['RelatedLink'] if group.include?('RelatedLinks')
      @adult_content = alexa['ContentData']['AdultContent'] if group.include?('AdultContent')

    end
   

    private

    def force_encoding(xml)
      if RUBY_VERSION >= '1.9'
        xml.force_encoding(Encoding::UTF_8)
      else
        xml
      end
    end

    def handle_response(response)
      case response.code.to_i
      when 200...300
        response
      when 300...600
        if response.body.nil?
          raise StandardError.new(response)
        else
          @xml_response = response.body
          xml = XmlSimple.xml_in(response.body, 'ForceArray' => false)
          message = xml['Errors']['Error']['Message']
          raise StandardError.new(message)
        end
      else
        raise StandardError.new("Unknown code: #{respnse.code}")
      end
    end

    def generate_signature(secret_access_key, action, timestamp)
      Base64.encode64( OpenSSL::HMAC.digest( OpenSSL::Digest::Digest.new( "sha1" ), secret_access_key, action + timestamp)).strip
    end

    def generate_url(action, access_key_id, signature, timestamp, response_group, host)
      url = URI.parse(
          "http://awis.amazonaws.com/?" +
          {
            "Action"         => action,
            "AWSAccessKeyId" => access_key_id,
            "Signature"      => signature,
            "Timestamp"      => timestamp,
            "ResponseGroup"  => response_group,
            "Url"            => host
          }.to_a.collect{ |item| item.first + "=" + CGI::escape(item.last) }.sort.join("&")
       )
    
    end
  end
end
