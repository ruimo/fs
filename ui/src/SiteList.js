import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './SiteList.css';
import MessagesLoader from "./MessagesLoader";
import SiteTable from "./SiteTable";
import SiteRepo from "./SiteRepo";

class SiteList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      messages: MessagesLoader.Empty,
      records: [],
      page: 0,
      pageSize: 10,
      orderBy: 'site.held_on_utc desc',
    };
  }

  onSiteSelected = (siteId) => {
    this.props.history.push("/agentRecords/" + siteId);
  }
  
  renderRecords = () => {
    SiteRepo.list(
      this.state.page, this.state.pageSize, this.state.orderBy,
      (table) => {
        this.setState({
          records: table
        });
      },
      () => {
        this.props.history.push("/login/site");
      },
      (msg) => {
        console.log(msg);
      }
    );
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  componentDidMount = async() => {
    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'siteList'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }

    this.renderRecords();
  }

  render() {
    return (
      <div className="siteList">
        <nav className="panel">
          <p className="panel-heading">
            {this.msg('siteList')}
          </p>

          <div className="panel-block">
            <SiteTable records={this.state.records} onSiteSelected={this.onSiteSelected} onSiteDeleted={this.onSiteDeleted}
                       canDeleteSite={false}/>
          </div>
        </nav>
      </div>
    );
  }
}

export default withRouter(SiteList);
